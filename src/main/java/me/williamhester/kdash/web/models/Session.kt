package me.williamhester.kdash.web.models

import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import java.time.Instant

data class Session(
  val sessionId: Int,
  val subSessionId: Int,
  val simSessionNumber: Int,
  val carNumber: String,
  val sessionMetadata: SessionMetadata,
  val sessionCreated: Instant?,
)
