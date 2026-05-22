package me.williamhester.kdash.web.extensions

import com.google.protobuf.Timestamp
import com.google.protobuf.util.Timestamps
import java.time.Instant

object ProtoExtensions {
  fun Instant?.toProtoTimestamp(): Timestamp {
    if (this == null) return Timestamp.getDefaultInstance()
    return Timestamps.fromMillis(this.toEpochMilli())
  }

  /**
   * Turn a FloatArray into an iterable to allow for `addAll` to be used
   *
   * Adding floats one-by-one to a list causes lists to be reallocated many times, which, for large lists, causes
   * pretty severe performance issues.
   */
  fun FloatArray.toIterable(end: Int = this.size) : Iterable<Float> {
    val array = this
    return object : Iterable<Float> {
      override fun iterator(): Iterator<Float> {
        return object : Iterator<Float> {
          var i = 0

          override fun hasNext(): Boolean = i < end

          override fun next(): Float = array[i++]
        }
      }
    }
  }

}
