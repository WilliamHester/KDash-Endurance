package me.williamhester.kdash.web.models

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot

data class TelemetryDataPoint(
  val sessionTime: Double,
  val driverDistance: Float,
  val dataSnapshot: DataSnapshot,
  val syntheticFields: SyntheticFields,
)
