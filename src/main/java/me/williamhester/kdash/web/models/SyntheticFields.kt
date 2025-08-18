package me.williamhester.kdash.web.models

data class SyntheticFields(
  val lastPitLap: Int,
  val estSpeed: Float,
)

class MutableSyntheticFields(
  var lastPitLap: Int = 0,
  var estSpeed: Float = 0.0F,
) {
  fun toSyntheticFields() = SyntheticFields(
    lastPitLap = lastPitLap,
    estSpeed = estSpeed,
  )
}
