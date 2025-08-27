package me.williamhester.kdash.web

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.web.models.SessionInfo
import me.williamhester.kdash.web.store.Store

class LiveTelemetryDataWriter(
  private val sessionInfo: SessionInfo,
) {
  fun onSessionMetadata(sessionMetadata: SessionMetadata) {
    Store.insertSessionMetadata(sessionInfo, sessionMetadata)
  }

  fun onDataSnapshot(dataSnapshot: DataSnapshot) {
    Store.insertDataSnapshot(sessionInfo, dataSnapshot)
  }
}
