package me.williamhester.kdash.web.store

import com.google.common.base.Joiner
import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.RateLimiter
import com.google.protobuf.Message
import com.impossibl.postgres.api.jdbc.PGConnection
import com.impossibl.postgres.api.jdbc.PGNotificationListener
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.enduranceweb.proto.Session
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.enduranceweb.proto.StintEntry
import me.williamhester.kdash.enduranceweb.proto.session
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.models.TelemetryDataPoint
import me.williamhester.kdash.web.models.TelemetryRange
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.max
import me.williamhester.kdash.enduranceweb.proto.TelemetryDataPoint as TelemetryDataPointProto

object Store {
  private val logger = FluentLogger.forEnclosingClass()
  private val broadcastingListener = BroadcastingListener()
  private val postgresUser by lazy {
    System.getenv("DB_USER") ?: System.getenv("USER") ?: "postgres"
  }
  private val connection: PGConnection by lazy {
    DriverManager.getConnection("jdbc:pgsql://localhost:5432/$postgresUser").unwrap(PGConnection::class.java).apply {
      addNotificationListener(broadcastingListener)
    }
  }
  private val executor = Executors.newCachedThreadPool()
  private const val LIMIT = 100_000

  fun insertTelemetryData(
    sessionKey: SessionKey,
    sessionTime: Double,
    driverDistance: Float,
    dataSnapshot: TelemetryDataPointProto,
  ) {
    insertOrSkip(
      Table.TELEMETRY_DATA,
      *sessionKey.toQueryParams(),
      "SessionTime" to sessionTime,
      "DriverDistance" to driverDistance,
      "Data" to dataSnapshot,
    )
  }

  fun insertLapEntry(sessionKey: SessionKey, lapEntry: LapEntry) {
    insertOrSkip(
      Table.DRIVER_LAPS,
      *sessionKey.toQueryParams(),
      "LapNum" to lapEntry.lapNum,
      "LapEntry" to lapEntry,
    )
  }

  fun insertStintEntry(sessionKey: SessionKey, stintEntry: StintEntry) {
    insertOrSkip(
      Table.DRIVER_STINTS,
      *sessionKey.toQueryParams(),
      "InLapNum" to stintEntry.inLap,
      "StintEntry" to stintEntry,
    )
  }

  fun insertOtherCarLapEntry(sessionKey: SessionKey, otherCarLapEntry: OtherCarLapEntry) {
    insertOrSkip(
      Table.OTHER_CAR_LAPS,
      *sessionKey.toQueryParams(),
      "OtherCarIdx" to otherCarLapEntry.carId,
      "LapNum" to otherCarLapEntry.lapNum,
      "LapEntry" to otherCarLapEntry,
    )
  }

  fun listSessionCars(): List<Session> {
    return executeQuery(
      """
        SELECT
          SessionID, 
          SubSessionID, 
          SimSessionNumber, 
          CarNumber,
          Metadata
        FROM SessionCars
      """.trimIndent(),
    ) {
      val sessionCars = mutableListOf<Session>()
      while (it.next()) {
        sessionCars += session {
          sessionId = it.getInt(1)
          subSessionId = it.getInt(2)
          simSessionNumber = it.getInt(3)
          carNumber = it.getString(4)
          // TODO: Probably move this somewhere else, but it's 12:35 AM on a Thursday night, before a race on Saturday.
          trackName = SessionMetadata.parseFrom(it.getBytes(5))["WeekendInfo"]["TrackDisplayName"].value
        }
      }
      return@executeQuery sessionCars
    }
  }

  fun insertSessionMetadata(sessionKey: SessionKey, sessionMetadata: SessionMetadata) {
    val queryParams = sessionKey.toQueryParams().map { Column(it.first, false) to it.second }.toMutableList()
    queryParams += Column("Metadata", true) to sessionMetadata
    insertOrUpdate(
      Table.SESSION_CARS,
      *queryParams.toTypedArray(),
    )
  }

  fun getMetadataForSession(sessionKey: SessionKey): SessionMetadata? {
    return executeQuery(
      """
        SELECT Metadata
        FROM SessionCars
        WHERE
          SessionID=? 
          AND SubSessionID=? 
          AND SimSessionNumber=? 
          AND CarNumber=?
      """.trimIndent(),
      sessionKey.sessionId,
      sessionKey.subSessionId,
      sessionKey.sessionNum,
      sessionKey.carNumber,
    ) {
      if (!it.next()) return@executeQuery null
      SessionMetadata.parseFrom(it.getBytes(1))
    }
  }

  fun getSessionTelemetryRange(sessionKey: SessionKey): TelemetryRange? {
    val (minSessionTime, maxSessionTime) = getMinAndMaxSessionTimes(sessionKey)
    if (minSessionTime == null || maxSessionTime == null) return null
    val (minDistance, maxDistance) = getMinAndMaxDriverDistances(sessionKey)
    if (minDistance == null || maxDistance == null) return null
    return TelemetryRange(minSessionTime, maxSessionTime, minDistance, maxDistance)
  }

  private fun getMinAndMaxSessionTimes(sessionKey: SessionKey) = executeQuery(
    """
        SELECT
          MIN(SessionTime),
          MAX(SessionTime)
        FROM TelemetryData
        WHERE
          SessionID = ?
          AND SubSessionID = ?
          AND SimSessionNumber = ?
          AND CarNumber = ?
      """.trimIndent(),
    sessionKey.sessionId,
    sessionKey.subSessionId,
    sessionKey.sessionNum,
    sessionKey.carNumber,
  ) {
    if (!it.next()) return@executeQuery null to null
    it.getDouble(1) to it.getDouble(2)
  }

  private fun getMinAndMaxDriverDistances(sessionKey: SessionKey): Pair<Float?, Float?> {
    val min = executeQuery(
      """
          SELECT DriverDistance
          FROM TelemetryData
          WHERE
            SessionID = ?
            AND SubSessionID = ?
            AND SimSessionNumber = ?
            AND CarNumber = ?
            AND DriverDistance > 0
          ORDER BY DriverDistance
          LIMIT 1
        """.trimIndent(),
      sessionKey.sessionId,
      sessionKey.subSessionId,
      sessionKey.sessionNum,
      sessionKey.carNumber,
    ) {
      if (!it.next()) return@executeQuery null
      it.getFloat(1)
    }
    val max = executeQuery(
      """
          SELECT DriverDistance
          FROM TelemetryData
          WHERE
            SessionID = ?
            AND SubSessionID = ?
            AND SimSessionNumber = ?
            AND CarNumber = ?
            AND DriverDistance > 0
          ORDER BY DriverDistance DESC
          LIMIT 1
        """.trimIndent(),
      sessionKey.sessionId,
      sessionKey.subSessionId,
      sessionKey.sessionNum,
      sessionKey.carNumber,
    ) {
      if (!it.next()) return@executeQuery null
      it.getFloat(1)
    }
    return min to max
  }

  private fun getDriverDistanceAtSessionTime(sessionKey: SessionKey, sessionTime: Double): Float? {
    return executeQuery("""
        SELECT DriverDistance
        FROM TelemetryData
        WHERE
            SessionID=? 
            AND SubSessionID=? 
            AND SimSessionNumber=? 
            AND CarNumber=?
            AND SessionTime=?
      """.trimIndent(),
      sessionKey.sessionId,
      sessionKey.subSessionId,
      sessionKey.sessionNum,
      sessionKey.carNumber,
      sessionTime) {
      if (!it.next()) return@executeQuery null
      it.getFloat(1)
    }
  }

  fun getTelemetryForRange(
    sessionKey: SessionKey,
    startTime: Double,
    endTime: Double,
    queryRateHz: Double? = null,
    responseListener: StreamedResponseListener<TelemetryDataPoint>,
  ) {
    // TODO: Fix a bug. If the "sessionNum" is -1, then this will fail to register the channel. Seems like "-" is an
    //  illegal character or needs to be escaped.
    val channelName = with(sessionKey) { "td_${sessionId}_${subSessionId}_${sessionNum}_${carNumber}" }
    val blockingQueue = LinkedBlockingQueue<String?>()

    broadcastingListener.register(channelName, blockingQueue).use {
      val rateLimiter = RateLimiter.create(queryRateHz ?: 60.0)
      var lastSessionTime = startTime
      while (!Thread.currentThread().isInterrupted) {
        rateLimiter.acquire()
        var rowsRead = 0
        executeQuery(
          """
          SELECT
            SessionTime,
            DriverDistance,
            Data
          FROM TelemetryData
          WHERE
            SessionID = ?
            AND SubSessionID = ? 
            AND SimSessionNumber = ? 
            AND CarNumber = ?
            AND SessionTime > ?
            AND SessionTime - 0.01 <= ?
          ORDER BY SessionTime
          LIMIT ?
        """.trimIndent(),
          sessionKey.sessionId,
          sessionKey.subSessionId,
          sessionKey.sessionNum,
          sessionKey.carNumber,
          lastSessionTime,
          endTime,
          LIMIT,
        ) {
          while (it.next() && !Thread.currentThread().isInterrupted) {
            rowsRead++
            lastSessionTime = it.getDouble(1)
            val telemetryDataPointProto = TelemetryDataPointProto.parseFrom(it.getBytes(3))
            responseListener.onNext(
              TelemetryDataPoint(
                lastSessionTime,
                it.getFloat(2),
                telemetryDataPointProto.dataSnapshot,
                telemetryDataPointProto.syntheticFields,
              )
            )
          }
        }
        if (lastSessionTime >= endTime - 0.001) break

        if (rowsRead == LIMIT) continue

        do {
          blockingQueue.take()
        } while (blockingQueue.isNotEmpty())
      }
    }
  }

  fun getSessionTimeForTargetDistance(sessionKey: SessionKey, targetDistance: Float): Double? {
    return executeQuery(
      """
        SELECT
          SessionTime
        FROM
          TelemetryData
        WHERE
          SessionID=?
          AND SubSessionID=?
          AND SimSessionNumber=?
          AND CarNumber=?
          AND DriverDistance<=?
        ORDER BY
          DriverDistance DESC
        LIMIT 1;
      """.trimIndent(),
      sessionKey.sessionId,
      sessionKey.subSessionId,
      sessionKey.sessionNum,
      sessionKey.carNumber,
      targetDistance,
    ) {
      if (!it.next()) return@executeQuery null
      return@executeQuery it.getDouble(1)
    }
  }

  fun getDriverLaps(sessionKey: SessionKey, responseListener: StreamedResponseListener<LapEntry>) {
    val channelName = with(sessionKey) { "dl_${sessionId}_${subSessionId}_${sessionNum}_${carNumber}" }
    val blockingQueue = LinkedBlockingQueue<String?>()

    broadcastingListener.register(channelName, blockingQueue).use {
      var lastLapId = -1
      while (!Thread.currentThread().isInterrupted) {
        executeQuery(
          """
          SELECT
            LapNum,
            LapEntry,
            LapID
          FROM
            DriverLaps
          WHERE
            SessionID=?
            AND SubSessionID=?
            AND SimSessionNumber=?
            AND CarNumber=?
            AND LapID>?
          ORDER BY LapID DESC
        """.trimIndent(),
          sessionKey.sessionId,
          sessionKey.subSessionId,
          sessionKey.sessionNum,
          sessionKey.carNumber,
          lastLapId,
        ) {
          while (it.next() && !Thread.currentThread().isInterrupted) {
            val newLapId = it.getInt(3)
            responseListener.onNext(LapEntry.parseFrom(it.getBytes(2)))
            lastLapId = max(newLapId, lastLapId)
          }

          do {
            val lapId = blockingQueue.take()!!.toInt()
          } while (lapId <= lastLapId)
        }
      }
    }
  }

  fun getDriverStints(sessionKey: SessionKey, responseListener: StreamedResponseListener<StintEntry>) {
    val channelName = with(sessionKey) { "ds_${sessionId}_${subSessionId}_${sessionNum}_${carNumber}" }
    val blockingQueue = LinkedBlockingQueue<String?>()

    broadcastingListener.register(channelName, blockingQueue).use {
      var lastStintId = -1
      while (!Thread.currentThread().isInterrupted) {
        executeQuery(
          """
        SELECT
          InLapNum,
          StintEntry,
          StintID
        FROM
          DriverStints
        WHERE
          SessionID=?
          AND SubSessionID=?
          AND SimSessionNumber=?
          AND CarNumber=?
          AND StintID>?
        ORDER BY StintID DESC
      """.trimIndent(),
          sessionKey.sessionId,
          sessionKey.subSessionId,
          sessionKey.sessionNum,
          sessionKey.carNumber,
          lastStintId
        ) {
          while (it.next() && !Thread.currentThread().isInterrupted) {
            val newStintId = it.getInt(3)
            responseListener.onNext(StintEntry.parseFrom(it.getBytes(2)))
            lastStintId = max(newStintId, lastStintId)
          }
        }

        do {
          val stintId = blockingQueue.take()!!.toInt()
        } while (stintId < lastStintId)
      }
    }
  }

  fun getOtherCarLaps(sessionKey: SessionKey, responseListener: StreamedResponseListener<OtherCarLapEntry>) {
    val channelName = with(sessionKey) { "ocl_${sessionId}_${subSessionId}_${sessionNum}_${carNumber}" }
    val blockingQueue = LinkedBlockingQueue<String?>()

    broadcastingListener.register(channelName, blockingQueue).use {
      var lastOtherCarLapId = -1

      while (!Thread.currentThread().isInterrupted) {
        executeQuery(
          """
        SELECT
          LapNum,
          LapEntry,
          LapID
        FROM
          OtherCarLaps
        WHERE
          SessionID=?
          AND SubSessionID=?
          AND SimSessionNumber=?
          AND CarNumber=?
          AND LapID>?
        ORDER BY LapID DESC
      """.trimIndent(),
          sessionKey.sessionId,
          sessionKey.subSessionId,
          sessionKey.sessionNum,
          sessionKey.carNumber,
          lastOtherCarLapId,
        ) {
          while (it.next() && !Thread.currentThread().isInterrupted) {
            val newLapId = it.getInt(3)
            responseListener.onNext(OtherCarLapEntry.parseFrom(it.getBytes(2)))
            lastOtherCarLapId = max(newLapId, lastOtherCarLapId)
          }
        }

        do {
          val lapId = blockingQueue.take()!!.toInt()
        } while (lapId < lastOtherCarLapId)
      }
    }
  }

  private fun insertOrSkip(table: Table, vararg args: Pair<String, Any>) {
    val columns = Joiner.on(", ").join(args.map { it.first })
    val questionMarks = Joiner.on(", ").join(args.map { "?" })
    val queryWithValuesString =
      "INSERT INTO ${table.tableName} ($columns) VALUES ($questionMarks) ON CONFLICT DO NOTHING"
    useStatement(queryWithValuesString, *args.map { it.second }.toTypedArray()) {
      it.executeUpdate()
    }
  }

  private data class Column(val name: String, val overwrite: Boolean)

  private fun insertOrUpdate(table: Table, vararg args: Pair<Column, Any>) {
    val columns = Joiner.on(", ").join(args.map { it.first.name })
    val keyColumns = args.filterNot { it.first.overwrite }.joinToString(", ") { it.first.name }
    val questionMarks = Joiner.on(", ").join(args.map { "?" })

    val argsToUpdate = args.filter { it.first.overwrite }
    val doPart = if (argsToUpdate.isNotEmpty()) {
      val setPart = argsToUpdate.joinToString(", ") { "${it.first.name} = EXCLUDED.${it.first.name}" }
      "UPDATE SET $setPart"
    } else {
      "NOTHING"
    }
    val queryWithValuesString =
      "INSERT INTO ${table.tableName} ($columns) VALUES ($questionMarks) ON CONFLICT ($keyColumns) DO $doPart"
    useStatement(queryWithValuesString, *args.map { it.second }.toTypedArray()) {
      it.executeUpdate()
    }
  }

  private fun <T> executeQuery(query: String, vararg args: Any, block: (ResultSet) -> T): T {
    logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log("Executing query: %s", query)
    return useStatement(query, *args) { statement ->
      statement.use {
        return@useStatement block(it.executeQuery())
      }
    }
  }

  private fun <T> useStatement(query: String, vararg args: Any, block: (PreparedStatement) -> T): T {
    return connection.prepareStatement(query).use { statement ->
      args.withIndex().forEach {
        val value = it.value
        val paramIndex = it.index + 1 // For some reason, JDBC uses 1-based indices
        when (value) {
          is String -> statement.setString(paramIndex, value)
          is Int -> statement.setInt(paramIndex, value)
          is Float -> statement.setFloat(paramIndex, value)
          is Double -> statement.setDouble(paramIndex, value)
          is Instant -> statement.setTimestamp(paramIndex, Timestamp.from(value))
          is Message -> statement.setBytes(paramIndex, value.toByteArray())
          else -> throw Exception("Unsupported arg type: ${value.javaClass.simpleName}")
        }
      }
      block(statement)
    }
  }

  private fun listen(channelName: String) {
    connection.createStatement().use { it.executeUpdate("LISTEN \"$channelName\"") }
  }

  private fun unlisten(channelName: String) {
    connection.createStatement().use { it.executeUpdate("UNLISTEN \"$channelName\"") }
  }

  private enum class Table(val tableName: String) {
    TELEMETRY_DATA("TelemetryData"),
    SESSION_CARS("SessionCars"),
    DRIVER_LAPS("DriverLaps"),
    DRIVER_STINTS("DriverStints"),
    OTHER_CAR_LAPS("OtherCarLaps"),
  }

  private class BroadcastingListener : PGNotificationListener {
    private val registry = ConcurrentHashMap<String, CopyOnWriteArrayList<LinkedBlockingQueue<String?>>>()

    fun register(channelName: String, blockingQueue: LinkedBlockingQueue<String?>): AutoCloseable {
      registry.compute(channelName) { key, oldValue ->
        val list =
          if (oldValue == null) {
            listen(channelName)
            CopyOnWriteArrayList<LinkedBlockingQueue<String?>>()
          } else {
            oldValue
          }
        list.add(blockingQueue)
        list
      }
      return AutoCloseable {
        registry.compute(channelName) { key, oldValue ->
          oldValue?.remove(blockingQueue)
          if (oldValue?.isNotEmpty() == true) {
            oldValue
          } else {
            unlisten(channelName)
            null
          }
        }
      }
    }

    override fun notification(processId: Int, channelName: String?, payload: String?) {
      if (channelName == null) return

      executor.submit {
        registry[channelName]?.map { it.add(payload) }
      }
    }
  }
}

private fun SessionKey.toQueryParams(): Array<Pair<String, Any>> {
  return arrayOf(
    "SessionID" to sessionId,
    "SubSessionID" to subSessionId,
    "SimSessionNumber" to sessionNum,
    "CarNumber" to carNumber,
  )
}
