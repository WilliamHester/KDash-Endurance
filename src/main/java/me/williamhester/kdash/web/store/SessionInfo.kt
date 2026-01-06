package me.williamhester.kdash.web.store

import me.williamhester.kdash.enduranceweb.proto.LookupTables
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata

data class SessionInfo(
  val metadata: SessionMetadata,
  val lookupTables: LookupTables,
)
