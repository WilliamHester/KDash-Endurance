package me.williamhester.kdash.web.service.telemetrypusher

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.monitors.DataSnapshotLogger
import me.williamhester.kdash.web.monitors.DistanceLookupTableMonitor
import me.williamhester.kdash.web.monitors.DriverCarLapLogger
import me.williamhester.kdash.web.monitors.MutableSyntheticFields
import me.williamhester.kdash.web.monitors.OtherCarsLapLogger
import me.williamhester.kdash.web.monitors.RelativeMonitor
import me.williamhester.kdash.web.monitors.RelativeMonitor2
import me.williamhester.kdash.web.state.MetadataHolder
import me.williamhester.kdash.web.store.SessionStore
import me.williamhester.kdash.web.store.Store

internal class LiveTelemetryDataWriter(
  private val sessionKey: SessionKey,
) {
  private val sessionStore = SessionStore(sessionKey)
  private val monitors = Monitors(sessionStore)

  fun onSessionMetadata(sessionMetadata: SessionMetadata) {
    monitors.metadataHolder.metadata = sessionMetadata
    Store.insertSessionMetadata(sessionKey, sessionMetadata)
  }

  fun onDataSnapshot(dataSnapshot: DataSnapshot) {
    monitors.process(dataSnapshot)
  }

  private class Monitors(sessionStore: SessionStore) {
    val metadataHolder = MetadataHolder()
    private val mutableSyntheticFields = MutableSyntheticFields()

    private val relativeMonitor = RelativeMonitor()
    private val lapMonitor = DriverCarLapLogger(metadataHolder, relativeMonitor, sessionStore, mutableSyntheticFields)
    private val otherCarsLapLogger = OtherCarsLapLogger(metadataHolder, relativeMonitor, sessionStore)
    private val dataSnapshotLogger = DataSnapshotLogger(metadataHolder, sessionStore, mutableSyntheticFields)
    private val relativeMonitor2 = RelativeMonitor2(metadataHolder, mutableSyntheticFields)
    private val distanceLookupTableMonitor = DistanceLookupTableMonitor(metadataHolder, sessionStore, relativeMonitor2)

    fun process(dataSnapshot: DataSnapshot) {
      relativeMonitor.process(dataSnapshot)
      lapMonitor.process(dataSnapshot)
      otherCarsLapLogger.process(dataSnapshot)
      dataSnapshotLogger.process(dataSnapshot)
      relativeMonitor2.process(dataSnapshot)
      distanceLookupTableMonitor.process(dataSnapshot)
    }
  }
}
