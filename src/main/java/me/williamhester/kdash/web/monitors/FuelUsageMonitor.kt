package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.state.MetadataHolder

class FuelUsageMonitor(
  private val metadataHolder: MetadataHolder,
) {
  private val _fuelLevelsByDistance = mutableListOf<FuelLevelByDistance>()
  val fuelLevelsByDistance: List<FuelLevelByDistance> = _fuelLevelsByDistance
  var ticksSeen = 0

  fun process(dataSnapshot: DataSnapshot) {
    if (ticksSeen++ % 15 != 0) return // Only collect at 4Hz
    val distance =
      dataSnapshot.lap + dataSnapshot.getCarIdxLapDistPct(metadataHolder.metadata["DriverInfo"]["DriverCarIdx"].value.toInt())
    val fuelLevel = dataSnapshot.fuelLevel
    dataSnapshot.lap

    _fuelLevelsByDistance.add(FuelLevelByDistance(distance, fuelLevel))
  }

  data class FuelLevelByDistance(val distance: Float, val fuelLevel: Float)
}