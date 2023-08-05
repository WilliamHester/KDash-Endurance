package me.williamhester.kdash.web.models

import java.time.Instant

data class Session(
  val sessionId: String,
  val userId: Int,
  val trackId: Int,
  val carId: Int,
  val trackName: String,
  val date: Instant, // Figure out what this should be
  val fastestLap: Double,
  val numLaps: Int,
)
