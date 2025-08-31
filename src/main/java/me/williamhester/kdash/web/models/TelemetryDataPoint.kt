package me.williamhester.kdash.web.models

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SyntheticFields

data class TelemetryDataPoint(
  val sessionTime: Double,
  val driverDistance: Float,
  val dataSnapshot: DataSnapshot,
  val syntheticFields: SyntheticFields,
)
