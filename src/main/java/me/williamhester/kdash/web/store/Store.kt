package me.williamhester.kdash.web.store

import com.google.common.base.Joiner
import com.google.protobuf.Message
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.web.models.SessionKey
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

object Store {
  private val connection: Connection by lazy {
    DriverManager.getConnection("jdbc:postgresql://localhost:5432/williamhester")
  }

  fun insertDataSnapshot(sessionKey: SessionKey, dataSnapshot: DataSnapshot) {
    insertOrUpdate(
      Table.TELEMETRY_DATA,
      "SessionID" to sessionKey.sessionId,
      "SubSessionID" to sessionKey.subSessionId,
      "SimSessionNumber" to sessionKey.sessionNum,
      "CarNumber" to sessionKey.carNumber,
      "SessionTime" to dataSnapshot.sessionTime,
      "Data" to dataSnapshot,
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
          is Double -> statement.setFloat(paramIndex, value.toFloat())
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
  }
}
