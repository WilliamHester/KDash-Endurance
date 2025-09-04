package me.williamhester.kdash.web.store

import com.google.common.base.Joiner
import com.google.common.util.concurrent.RateLimiter
import com.google.protobuf.Message
import com.impossibl.postgres.api.jdbc.PGConnection
import com.impossibl.postgres.api.jdbc.PGNotificationListener
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.enduranceweb.proto.StintEntry
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
import me.williamhester.kdash.enduranceweb.proto.TelemetryDataPoint as TelemetryDataPointProto

object Store {
  private val broadcastingListener = BroadcastingListener()
  private val connection: PGConnection by lazy {
    DriverManager.getConnection("jdbc:pgsql://localhost:5432/williamhester").unwrap(PGConnection::class.java).apply {
      addNotificationListener(broadcastingListener)
    }
  }
  private val executor = Executors.newCachedThreadPool()

  fun insertTelemetryData(
    sessionKey: SessionKey,
    sessionTime: Double,
    driverDistance: Float,
    dataSnapshot: TelemetryDataPointProto,
  ) {
    insertOrUpdate(
      Table.TELEMETRY_DATA,
      *sessionKey.toQueryParams(),
      "SessionTime" to sessionTime,
      "DriverDistance" to driverDistance,
      "Data" to dataSnapshot,
    )
  }

  fun insertLapEntry(sessionKey: SessionKey, lapEntry: LapEntry) {
    insertOrUpdate(
      Table.DRIVER_LAPS,
      *sessionKey.toQueryParams(),
      "LapNum" to lapEntry.lapNum,
      "LapEntry" to lapEntry,
    )
  }

  fun insertStintEntry(sessionKey: SessionKey, stintEntry: StintEntry) {
    insertOrUpdate(
      Table.DRIVER_STINTS,
      *sessionKey.toQueryParams(),
      "InLapNum" to stintEntry.inLap,
      "StintEntry" to stintEntry,
    )
  }

  fun insertOtherCarLapEntry(sessionKey: SessionKey, otherCarLapEntry: OtherCarLapEntry) {
    insertOrUpdate(
      Table.OTHER_CAR_LAPS,
      *sessionKey.toQueryParams(),
      "OtherCarIdx" to otherCarLapEntry.carId,
      "LapNum" to otherCarLapEntry.lapNum,
      "LapEntry" to otherCarLapEntry,
    )
  }

  fun insertSessionMetadata(sessionKey: SessionKey, sessionMetadata: SessionMetadata) {
    insertOrUpdate(
      Table.SESSION_CARS,
      *sessionKey.toQueryParams(),
      "Metadata" to sessionMetadata,
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
    val (minSessionTime, maxSessionTime) = executeQuery("""
      SELECT
        MIN(SessionTime),
        MAX(SessionTime)
      FROM TelemetryData
      WHERE
          SessionID=? 
          AND SubSessionID=? 
          AND SimSessionNumber=? 
          AND CarNumber=?
    """.trimIndent(),
      sessionKey.sessionId,
      sessionKey.subSessionId,
      sessionKey.sessionNum,
      sessionKey.carNumber) {
      if (!it.next()) return@executeQuery null to null
      it.getDouble(1) to it.getDouble(2)
    }
    if (minSessionTime == null || maxSessionTime == null) return null
    val minDistance = getDriverDistanceAtSessionTime(sessionKey, minSessionTime)
    val maxDistance = getDriverDistanceAtSessionTime(sessionKey, maxSessionTime)
    if (minDistance == null || maxDistance == null) return null
    return TelemetryRange(minSessionTime, maxSessionTime, minDistance, maxDistance)
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
    val channelName = with(sessionKey) { "td_${sessionId}_${subSessionId}_${sessionNum}_${carNumber}" }
    val blockingQueue = LinkedBlockingQueue<String?>()

    broadcastingListener.register(channelName, blockingQueue).use {
      val rateLimiter = RateLimiter.create(queryRateHz ?: 60.0)
      var lastSessionTime = startTime
      while (!Thread.currentThread().isInterrupted) {
        rateLimiter.acquire()
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
        """.trimIndent(),
          sessionKey.sessionId,
          sessionKey.subSessionId,
          sessionKey.sessionNum,
          sessionKey.carNumber,
          lastSessionTime,
          endTime,
        ) {
          while (it.next()) {
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
    executeQuery("""
        SELECT
          LapNum,
          LapEntry
        FROM
          DriverLaps
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
      while (it.next()) {
        responseListener.onNext(LapEntry.parseFrom(it.getBytes(2)))
      }
    }
  }

  fun getDriverStints(sessionKey: SessionKey, responseListener: StreamedResponseListener<StintEntry>) {
    executeQuery("""
        SELECT
          InLapNum,
          StintEntry
        FROM
          DriverStints
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
      while (it.next()) {
        responseListener.onNext(StintEntry.parseFrom(it.getBytes(2)))
      }
    }
  }

  fun getOtherCarLaps(sessionKey: SessionKey, responseListener: StreamedResponseListener<OtherCarLapEntry>) {
    executeQuery("""
        SELECT
          LapNum,
          LapEntry
        FROM
          OtherCarLaps
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
      while (it.next()) {
        responseListener.onNext(OtherCarLapEntry.parseFrom(it.getBytes(2)))
      }
    }
  }

  private fun insertOrUpdate(table: Table, vararg args: Pair<String, Any>) {
    val columns = Joiner.on(", ").join(args.map { it.first })
    val questionMarks = Joiner.on(", ").join(args.map { "?" })
    val queryWithValuesString =
      "INSERT INTO ${table.tableName} ($columns) VALUES ($questionMarks) ON CONFLICT DO NOTHING"
    useStatement(queryWithValuesString, *args.map { it.second }.toTypedArray()) {
      it.executeUpdate()
    }
  }

  private fun <T> executeQuery(query: String, vararg args: Any, block: (ResultSet) -> T): T {
    return useStatement(query, *args) {
      return@useStatement block(it.executeQuery())
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
    connection.createStatement().use { it.executeUpdate("LISTEN $channelName") }
  }

  private fun unlisten(channelName: String) {
    connection.createStatement().use { it.executeUpdate("UNLISTEN $channelName") }
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
