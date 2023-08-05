package me.williamhester.kdash.web.store

import com.google.common.base.Joiner
import me.williamhester.kdash.web.models.Account
import me.williamhester.kdash.web.models.Session
import me.williamhester.kdash.web.models.TargetLap
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

object Store {
  private val connection: Connection
    get() = ConnectionProvider.get()

  fun createUser(
    username: String,
    email: String,
    passwordHash: String,
    firstName: String,
    lastName: String,
  ): Account {
    insertOrUpdate(
      Table.USERS,
      "Username" to username,
      "Email" to email,
      "Password" to passwordHash,
      "FirstName" to firstName,
      "LastName" to lastName,
    )
    return getUser(username)!!
  }

  fun getUser(userId: Int): Account? {
    return executeQuery("SELECT * FROM Users WHERE UserId=?", userId) {
      if (!it.next()) return@executeQuery null

      accountFromResultSet(it)
    }
  }

  fun getUser(username: String): Account? {
    return executeQuery("SELECT * FROM Users WHERE Username=?", username) {
      if (!it.next()) return@executeQuery null

      accountFromResultSet(it)
    }
  }

  private fun accountFromResultSet(it: ResultSet) = Account(
    userId = it.getInt("UserId"),
    username = it.getString("Username"),
    passwordHash = it.getString("Password"),
    firstName = it.getString("FirstName"),
    lastName = it.getString("LastName"),
  )

  fun getSessions(userId: Int): List<Session> {
    return executeQuery("SELECT * FROM Sessions WHERE UserId=?", userId) {
      val sessions = mutableListOf<Session>()
      while (it.next()) {
        sessions.add(
          getSessionFromCursor(it)
        )
      }
      return@executeQuery sessions
    }
  }

  fun getSession(sessionId: String): Session? {
    return executeQuery("SELECT * FROM Sessions WHERE SessionId=?", sessionId) {
      if (!it.next()) return@executeQuery null

      return@executeQuery getSessionFromCursor(it)
    }
  }

  private fun getSessionFromCursor(it: ResultSet): Session {
    return Session(
      sessionId = it.getString("SessionId"),
      userId = it.getInt("UserId"),
      trackId = it.getInt("TrackId"),
      carId = it.getInt("CarId"),
      trackName = it.getString("TrackName"),
      date = it.getTimestamp("SessionDate").toInstant(),
      fastestLap = it.getDouble("FastestLap"),
      numLaps = it.getInt("NumLaps"),
    )
  }

  fun createSession(session: Session) {
    insertOrUpdate(
      Table.SESSIONS,
      "SessionId" to session.sessionId,
      "UserId" to session.userId,
      "TrackId" to session.trackId,
      "TrackName" to session.trackName,
      "CarId" to session.carId,
      "SessionDate" to session.date,
      "FastestLap" to session.fastestLap,
      "NumLaps" to session.numLaps,
    )
  }

  fun createUserSession(userId: Int, sessionId: String) {
    insertOrUpdate(
      Table.USER_SESSIONS,
      "SessionId" to sessionId,
      "UserId" to userId,
    )
  }

  fun getUserSession(sessionId: String): Int? {
    return executeQuery("SELECT * FROM UserSessions WHERE SessionId=?", sessionId) {
      if (!it.next()) return@executeQuery null

      it.getInt("UserId")
    }
  }

  fun setTargetLapForUser(userId: Int, trackId: Int, carId: Int, lapSessionId: String, lapNum: Int) {
    insertOrUpdate(
      Table.USER_TARGET_LAPS,
      "UserId" to userId,
      "TrackId" to trackId,
      "CarId" to carId,
      "LapSessionId" to lapSessionId,
      "LapNum" to lapNum,
    )
  }

  fun getTargetLapForUser(userId: Int, trackId: Int, carId: Int): TargetLap? {
    return executeQuery(
      "SELECT LapSessionId, LapNum FROM UserTargetLaps WHERE UserId=? AND TrackId=? AND CarId=?",
      userId,
      trackId,
      carId,
    ) {
      if (!it.next()) return@executeQuery null

      TargetLap(it.getString("LapSessionId"), it.getInt("LapNum"))
    }
  }

  private fun insertOrUpdate(table: Table, vararg args: Pair<String, Any>) {
    val columns = Joiner.on(", ").join(args.map { it.first })
    val questionMarks = Joiner.on(", ").join(args.map { "?" })
    val queryWithValuesString = "REPLACE INTO ${table.tableName} ($columns) VALUES ($questionMarks)"
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
          else -> throw Exception("Unsupported arg type: ${value.javaClass.simpleName}")
        }
      }
      block(statement)
    }
  }

  private enum class Table(val tableName: String) {
    USERS("Users"),
    SESSIONS("Sessions"),
    USER_SESSIONS("UserSessions"),
    USER_TARGET_LAPS("UserTargetLaps"),
  }
}