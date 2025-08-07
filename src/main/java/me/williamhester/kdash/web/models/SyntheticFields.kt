package me.williamhester.kdash.web.models

data class SyntheticFields(
  val lastPitLap: Int,
)

class MutableSyntheticFields(
  var lastPitLap: Int = 0,
) {
  fun toSyntheticFields() = SyntheticFields(
    lastPitLap = lastPitLap,
  )
}
