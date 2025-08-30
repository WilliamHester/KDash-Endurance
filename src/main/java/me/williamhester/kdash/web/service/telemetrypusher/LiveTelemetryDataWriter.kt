package me.williamhester.kdash.web.service.telemetrypusher

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.monitors.DataSnapshotMonitor
import me.williamhester.kdash.web.monitors.DriverCarLapMonitor
import me.williamhester.kdash.web.monitors.DriverDistancesMonitor
import me.williamhester.kdash.web.monitors.DriverMonitor
import me.williamhester.kdash.web.monitors.OtherCarsLapMonitor
import me.williamhester.kdash.web.monitors.RelativeMonitor
import me.williamhester.kdash.web.state.MetadataHolder
import me.williamhester.kdash.web.store.Store

internal class LiveTelemetryDataWriter(
  private val sessionKey: SessionKey,
) {
  private lateinit var monitors: Monitors

  fun onSessionMetadata(sessionMetadata: SessionMetadata) {
    monitors = Monitors(sessionMetadata)
    monitors.metadataHolder.metadata = sessionMetadata
    Store.insertSessionMetadata(sessionKey, sessionMetadata)
  }

  fun onDataSnapshot(dataSnapshot: DataSnapshot) {
    monitors.process(dataSnapshot)
    Store.insertDataSnapshot(sessionKey, dataSnapshot)
  }

  private class Monitors(initialMetadata: SessionMetadata) {
    val metadataHolder = MetadataHolder(initialMetadata)

    private val relativeMonitor = RelativeMonitor()
    private val lapMonitor = DriverCarLapMonitor(metadataHolder, relativeMonitor)
    private val otherCarsLapMonitor = OtherCarsLapMonitor(metadataHolder, relativeMonitor)
    private val driverDistancesMonitor = DriverDistancesMonitor()
    private val dataSnapshotMonitor = DataSnapshotMonitor(metadataHolder)
    private val driverMonitor = DriverMonitor(metadataHolder)

    fun process(dataSnapshot: DataSnapshot) {
      relativeMonitor.process(dataSnapshot)
      lapMonitor.process(dataSnapshot)
      otherCarsLapMonitor.process(dataSnapshot)
      driverDistancesMonitor.process(dataSnapshot)
      dataSnapshotMonitor.process(dataSnapshot)
    }
  }
}
