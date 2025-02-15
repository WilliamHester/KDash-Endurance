package me.williamhester.kdash.web.extensions

import me.williamhester.kdash.enduranceweb.proto.SessionMetadata

operator fun SessionMetadata.get(index: Int): SessionMetadata = this.getList(index)

operator fun SessionMetadata.get(key: String): SessionMetadata =
  this.keyValuePairsMap[key] ?: SessionMetadata.getDefaultInstance()
