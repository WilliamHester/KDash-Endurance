package me.williamhester.kdash.web.store

import com.google.common.base.Joiner
import com.google.protobuf.Message
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.enduranceweb.proto.StintEntry
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.models.TelemetryDataPoint
import me.williamhester.kdash.web.models.TelemetryRange
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import me.williamhester.kdash.enduranceweb.proto.TelemetryDataPoint as TelemetryDataPointProto

object Store {
  private val connection: Connection by lazy {
    DriverManager.getConnection("jdbc:postgresql://localhost:5432/williamhester")
  }

  fun insertTelemetryData(
    sessionKey: SessionKey,
    sessionTime: Double,
    driverDistance: Float,
    dataSnapshot: TelemetryDataPointProto,
  ) {
    insertOrUpdate(
      Table.TELEMETRY_DATA,
      "SessionID" to sessionKey.sessionId,
      "SubSessionID" to sessionKey.subSessionId,
      "SimSessionNumber" to sessionKey.sessionNum,
      "CarNumber" to sessionKey.carNumber,
      "SessionTime" to sessionTime,
      "DriverDistance" to driverDistance,
      "Data" to dataSnapshot,
    )
  }

  fun insertLapEntry(sessionKey: SessionKey, lapEntry: LapEntry) {
    insertOrUpdate(
      Table.DRIVER_LAPS,
      "SessionID" to sessionKey.sessionId,
      "SubSessionID" to sessionKey.subSessionId,
      "SimSessionNumber" to sessionKey.sessionNum,
      "CarNumber" to sessionKey.carNumber,
      "LapNum" to lapEntry.lapNum,
      "LapEntry" to lapEntry,
    )
  }

  fun insertStintEntry(sessionKey: SessionKey, stintEntry: StintEntry) {
    insertOrUpdate(
      Table.DRIVER_STINTS,
      "SessionID" to sessionKey.sessionId,
      "SubSessionID" to sessionKey.subSessionId,
      "SimSessionNumber" to sessionKey.sessionNum,
      "CarNumber" to sessionKey.carNumber,
      "InLapNum" to stintEntry.inLap,
      "StintEntry" to stintEntry,
    )
  }

  fun insertOtherCarLapEntry(sessionKey: SessionKey, otherCarLapEntry: OtherCarLapEntry) {
    insertOrUpdate(
      Table.OTHER_CAR_LAPS,
      "SessionID" to sessionKey.sessionId,
      "SubSessionID" to sessionKey.subSessionId,
      "SimSessionNumber" to sessionKey.sessionNum,
      "CarNumber" to sessionKey.carNumber,
      "OtherCarIdx" to otherCarLapEntry.carId,
      "LapNum" to otherCarLapEntry.lapNum,
      "LapEntry" to otherCarLapEntry,
    )
  }

  fun insertSessionMetadata(sessionKey: SessionKey, sessionMetadata: SessionMetadata) {
    insertOrUpdate(
      Table.SESSION_CARS,
      "SessionID" to sessionKey.sessionId,
      "SubSessionID" to sessionKey.subSessionId,
      "SimSessionNumber" to sessionKey.sessionNum,
      "CarNumber" to sessionKey.carNumber,
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
    responseListener: StreamedResponseListener<TelemetryDataPoint>,
  ) {
    return executeQuery("""
        SELECT
          SessionTime,
          DriverDistance,
          Data
        FROM TelemetryData
        WHERE
          SessionID=? 
          AND SubSessionID=? 
          AND SimSessionNumber=? 
          AND CarNumber=?
          AND SessionTime>?
          AND SessionTime<?
      """.trimIndent(),
      sessionKey.sessionId,
      sessionKey.subSessionId,
      sessionKey.sessionNum,
      sessionKey.carNumber,
      startTime,
      endTime,
    ) {
      var sessionTime: Double? = null
      while (it.next()) {
        sessionTime = it.getDouble(1)
        val telemetryDataPointProto = TelemetryDataPointProto.parseFrom(it.getBytes(3))
        responseListener.onNext(
          TelemetryDataPoint(
            sessionTime,
            it.getFloat(2),
            telemetryDataPointProto.dataSnapshot,
            telemetryDataPointProto.syntheticFields,
          )
        )
      }
    }
  }

  fun interface StreamedResponseListener<T> {
    fun onNext(value: T)
  }

  fun getSessionTimeForTargetDistance(sessionKey: SessionKey, targetDistance: Float): Double? {
    return executeQuery("""
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

  private enum class Table(val tableName: String) {
    TELEMETRY_DATA("TelemetryData"),
    SESSION_CARS("SessionCars"),
    DRIVER_LAPS("DriverLaps"),
    DRIVER_STINTS("DriverStints"),
    OTHER_CAR_LAPS("OtherCarLaps"),
  }
}
