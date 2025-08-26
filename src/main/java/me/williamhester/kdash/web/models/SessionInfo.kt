package me.williamhester.kdash.web.models

data class SessionInfo(
  val sessionId: Int,
  val subSessionId: Int,
  val sessionNum: Int,
  val carNumber: String,
)
