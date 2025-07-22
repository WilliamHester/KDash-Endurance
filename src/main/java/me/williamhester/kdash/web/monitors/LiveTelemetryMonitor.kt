package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.TelemetryData
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.state.MetadataHolder

class LiveTelemetryMonitor(
  private val metadataHolder: MetadataHolder,
) {
  private val _telemetryData = mutableListOf<TelemetryData>()
  val telemetryData: List<TelemetryData> = _telemetryData

  fun process(dataSnapshot: DataSnapshot) {
    val driverCarIdx = metadataHolder.metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
    val sessionTime = dataSnapshot.sessionTime

    val telemetryDataBuilder = TelemetryData.newBuilder().apply {
      this.sessionTime = sessionTime
      driverDistance = dataSnapshot.lap + dataSnapshot.getCarIdxLapDistPct(driverCarIdx)
    }

    telemetryDataBuilder.setFuelLevel(dataSnapshot.fuelLevel)

    _telemetryData.add(telemetryDataBuilder.build())
  }
}