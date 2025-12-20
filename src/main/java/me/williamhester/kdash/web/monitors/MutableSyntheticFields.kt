package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.syntheticFields

class MutableSyntheticFields(
  var lastPitLap: Int = 0,
  var estSpeed: Float = 0.0F,
  var trackPrecip: Double = 0.0,
  var requiredRepairsRemaining: Float = 0.0F,
  var optionalRepairsRemaining: Float = 0.0F,
  var lapFuelUsed: Float = 0.0F,
) {
  fun toSyntheticFields() = syntheticFields {
    lastPitLap = this@MutableSyntheticFields.lastPitLap
    estSpeed = this@MutableSyntheticFields.estSpeed
    trackPrecip = this@MutableSyntheticFields.trackPrecip
    requiredRepairsRemaining = this@MutableSyntheticFields.requiredRepairsRemaining
    optionalRepairsRemaining = this@MutableSyntheticFields.optionalRepairsRemaining
    lapFuelUsed = this@MutableSyntheticFields.lapFuelUsed
  }
}
