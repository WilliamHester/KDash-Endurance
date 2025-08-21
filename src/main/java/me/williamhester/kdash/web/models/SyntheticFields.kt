package me.williamhester.kdash.web.models

data class SyntheticFields(
  val lastPitLap: Int,
  val estSpeed: Float,
  val trackPrecip: Double,
)

class MutableSyntheticFields(
  var lastPitLap: Int = 0,
  var estSpeed: Float = 0.0F,
  var trackPrecip: Double = 0.0,
) {
  fun toSyntheticFields() = SyntheticFields(
    lastPitLap = lastPitLap,
    estSpeed = estSpeed,
    trackPrecip = trackPrecip,
  )
}
