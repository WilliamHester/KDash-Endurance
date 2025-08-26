package me.williamhester.kdash.web.store

import com.google.common.base.Joiner
import com.google.protobuf.Message
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.models.SessionInfo
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

object Store {
  private val connection: Connection
    get() = ConnectionProvider.get()

  fun insertDataSnapshot(sessionInfo: SessionInfo, dataSnapshot: DataSnapshot) {
    insertOrUpdate(
      Table.TELEMETRY_DATA,
      "SessionID" to sessionInfo.sessionId,
      "SubSessionID" to sessionInfo.subSessionId,
      "SimSessionNumber" to sessionInfo.sessionNum,
      "CarNumber" to sessionInfo.carNumber,
      "SessionTime" to dataSnapshot.sessionTime,
      "Data" to dataSnapshot,
    )
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
  }
}
