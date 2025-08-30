package me.williamhester.kdash.web.models

/** Key for identifying a single session. */
data class SessionKey(
  val sessionId: Int,
  val subSessionId: Int,
  val sessionNum: Int,
  val carNumber: String,
)
