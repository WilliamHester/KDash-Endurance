package me.williamhester.kdash.web.session

import me.williamhester.kdash.web.store.Store
import java.security.SecureRandom

object UserSessionManager {
  private val secureRandom = SecureRandom()

  fun get(key: String): Int? {
    return Store.getUserSession(key)
  }

  fun newSession(userId: Int): String {
    val sessionId = secureRandom.nextLong().toString()
    Store.createUserSession(userId, sessionId)
    return sessionId
  }
}