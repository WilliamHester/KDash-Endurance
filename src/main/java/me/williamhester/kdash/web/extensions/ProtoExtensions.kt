package me.williamhester.kdash.web.extensions

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import java.time.Instant

object ProtoExtensions {
  fun Instant?.toProtoTimestamp(): Timestamp {
    if (this == null) return Timestamp.getDefaultInstance()
    return Timestamps.fromMillis(this.toEpochMilli())
  }
}
