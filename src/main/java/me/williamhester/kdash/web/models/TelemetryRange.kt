package me.williamhester.kdash.web.models

data class TelemetryRange(
  val minSessionTime: Double,
  val maxSessionTime: Double,
  val minDriverDistance: Float,
  val maxDriverDistance: Float,
)
