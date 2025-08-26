package me.williamhester.kdash.web

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.models.SessionInfo
import me.williamhester.kdash.web.store.Store

class LiveTelemetryDataWriter(
  private val sessionInfo: SessionInfo,
) {
  fun onDataSnapshot(dataSnapshot: DataSnapshot) {
    Store.insertDataSnapshot(sessionInfo, dataSnapshot)
  }
}
