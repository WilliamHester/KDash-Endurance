package me.williamhester.kdash.web.service.telemetrypusher

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.monitors.DataSnapshotLogger
import me.williamhester.kdash.web.monitors.DriverCarLapLogger
import me.williamhester.kdash.web.monitors.OtherCarsLapLogger
import me.williamhester.kdash.web.monitors.RelativeMonitor
import me.williamhester.kdash.web.state.MetadataHolder
import me.williamhester.kdash.web.store.SessionStore
import me.williamhester.kdash.web.store.Store

internal class LiveTelemetryDataWriter(
  private val sessionKey: SessionKey,
) {
  private val sessionStore = SessionStore(sessionKey)
  private lateinit var monitors: Monitors

  fun onSessionMetadata(sessionMetadata: SessionMetadata) {
    monitors = Monitors(sessionMetadata, sessionStore)
    monitors.metadataHolder.metadata = sessionMetadata
    Store.insertSessionMetadata(sessionKey, sessionMetadata)
  }

  fun onDataSnapshot(dataSnapshot: DataSnapshot) {
    monitors.process(dataSnapshot)
  }

  private class Monitors(
    initialMetadata: SessionMetadata,
    sessionStore: SessionStore,
  ) {
    val metadataHolder = MetadataHolder(initialMetadata)

    private val relativeMonitor = RelativeMonitor()
    private val lapMonitor = DriverCarLapLogger(metadataHolder, relativeMonitor, sessionStore)
    private val otherCarsLapLogger = OtherCarsLapLogger(metadataHolder, relativeMonitor, sessionStore)
    private val dataSnapshotLogger = DataSnapshotLogger(metadataHolder, sessionStore)

    fun process(dataSnapshot: DataSnapshot) {
      relativeMonitor.process(dataSnapshot)
      lapMonitor.process(dataSnapshot)
      otherCarsLapLogger.process(dataSnapshot)
      dataSnapshotLogger.process(dataSnapshot)
    }
  }
}
