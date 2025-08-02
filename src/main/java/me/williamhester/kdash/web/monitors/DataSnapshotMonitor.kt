package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.TelemetryDataPoint
import me.williamhester.kdash.web.state.MetadataHolder

class DataSnapshotMonitor(
  private val metadataHolder: MetadataHolder,
) {
  private val _telemetryDataPoints = mutableListOf<TelemetryDataPoint>()
  val telemetryDataPoints: List<TelemetryDataPoint> = _telemetryDataPoints

  fun process(dataSnapshot: DataSnapshot) {
    val driverCarIdx = metadataHolder.metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
    val sessionTime = dataSnapshot.sessionTime
    val driverDistance = dataSnapshot.lap + dataSnapshot.getCarIdxLapDistPct(driverCarIdx)

    _telemetryDataPoints.add(TelemetryDataPoint(sessionTime, driverDistance, dataSnapshot))
  }
}
