package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LookupTable
import me.williamhester.kdash.enduranceweb.proto.lookupTable
import me.williamhester.kdash.web.common.Interpolation.interpolate
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.extensions.getCarIdx
import me.williamhester.kdash.web.state.MetadataHolder

/**
 *
 */
class CarIdxDistanceLookupTableMonitor(
  metadataHolder: MetadataHolder,
  private val carIdx: Int,
) {
  private val buckets: Int by lazy {
    (metadataHolder.metadata.getCarIdx(carIdx)!!["CarClassEstLapTime"].value.toFloat() *
        1.5 *
        BUCKETS_PER_SECOND).toInt()
  }
  private val carIdxEstDistanceTimeAtTime: FloatArray by lazy {
    FloatArray(buckets)
  }
  private var currentMaxEstTimeBucket = 0.0F
  private var lastEstTime = 0.0F
  private var lastTimeBucket = 0.0F
  private var lastLapDistPct = 0.0F
  private var lastTotalDistancePct = 0.0F

  fun process(dataSnapshot: DataSnapshot): Float {
    val estTime = dataSnapshot.getCarIdxEstTime(carIdx)
    val lapDistPct = dataSnapshot.getCarIdxLapDistPct(carIdx)
    val lap = dataSnapshot.getCarIdxLap(carIdx)

    val totalDistancePct = lap + lapDistPct
    if (totalDistancePct < lastTotalDistancePct) {
      // The car is going backwards. Skip this data, since it would blow away the array.
      return lastEstTime
    }
    if (lapDistPct < 0) {
      // The car is probably off track or there's some other oddity about the input data. Skip this snapshot.
      return lastEstTime
    }

    val timeBucket = estTime * BUCKETS_PER_SECOND

    if (timeBucket < lastTimeBucket) {
      // This is fairly crude, but it allows us to wrap around without any complicated lookups in the session metadata,
      // which might be stale anyway. The downside is that we lose some accuracy at the end of a lap, but only by a few
      // hundredths of a second, so whatever.
      currentMaxEstTimeBucket = lastTimeBucket
    }
    if (timeBucket > currentMaxEstTimeBucket) {
      currentMaxEstTimeBucket = timeBucket
    }

    for (i in bucketsBetween(lastTimeBucket, timeBucket)) {
      val index = i % currentMaxEstTimeBucket.toInt()
      carIdxEstDistanceTimeAtTime[index] =
        interpolate(i.toFloat(), lastTimeBucket, timeBucket, lastLapDistPct, lapDistPct)
    }

    lastEstTime = estTime
    lastTimeBucket = timeBucket
    lastLapDistPct = lapDistPct
    lastTotalDistancePct = totalDistancePct
    return estTime
  }

  fun getLookupTable(): LookupTable = lookupTable {
    for (i in 0 until currentMaxEstTimeBucket.toInt()) {
      values.add(carIdxEstDistanceTimeAtTime[i])
    }
  }

  /** Range of ints [lastDistance, newDistance) */
  private fun bucketsBetween(lastBucket: Float, newBucket: Float): List<Int> {
    val start = lastBucket.toInt()
    val end = newBucket.toInt()
    if (start > end) {
      // We didn't cross any integers.
      return emptyList()
    }
    return (start..end).toList()
  }

  companion object {
    private const val BUCKETS_PER_SECOND = 60
  }
}