package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.common.Interpolation.interpolate
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.state.MetadataHolder

/**
 * A monitor that ingests the current driver's position and estimated time and produces a new synthetic variable that
 * is the estimated time for each car at a specific point on track.
 *
 * Important considerations:
 *  - The expected pace of the driver changes based on current conditions (i.e. wet laps will be slower)
 *  - Not all cars are in the same class, nor do cars in the same class necessarily have the same estimated time at a
 *  given position
 *  - Drivers may make mistakes, so we need to use their expected time to reach a point on track, not the actual time.
 *  Otherwise, a spin or slowdown would make it look like the track got artificially slower for everyone.
 *
 * This creates a ring buffer for the driver's expected lap time, as it passes through each point on track. From this,
 * we can the CarIdxLapDistPct field to look up what each car's expected lap time is if they were in the driver's car at
 * that position. It updates as the driver makes progress throughout the race, so as conditions change, the expected lap
 * times at each position also change.
 *
 * This uses buckets, spaced in 1 meter increments throughout the whole track length, giving us a pretty high
 * granularity.
 */
class RelativeMonitor2(
  metadataHolder: MetadataHolder,
  private val mutableSyntheticFields: MutableSyntheticFields,
) {
  private val trackLengthMeters: Int by lazy {
    (metadataHolder.metadata["WeekendInfo"]["TrackLength"].value.substringBefore(" km").toDouble() * 1000).toInt()
  }
  private val driverCarIdx: Int by lazy {
    metadataHolder.metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
  }
  internal val driverCarEstTimeAtDistance: FloatArray by lazy {
    FloatArray(trackLengthMeters)
  }
  private var lastEstTime = 0.0F
  private var lastLapDistMeters = 0.0F
  private var lastLapDistPct = 0.0F
  private var lastTotalDistancePct = 0.0F

  fun process(dataSnapshot: DataSnapshot) {
    val estTime = dataSnapshot.getCarIdxEstTime(driverCarIdx)
    val lapDistPct = dataSnapshot.lapDistPct
    val lap = dataSnapshot.lap

    val totalDistancePct = lap + lapDistPct
    if (totalDistancePct < lastTotalDistancePct) {
      // We're going backwards. Skip this data, since it would blow away the array.
      return
    }
    if (lapDistPct < 0) {
      // We're probably off track or there's some other oddity about the input data. Skip this snapshot.
      return
    }

    val lapDistMeters = lapDistPct * trackLengthMeters

    for (i in intsBetween(lastLapDistMeters, lapDistMeters)) {
      driverCarEstTimeAtDistance[i % trackLengthMeters] =
        interpolate(i.toFloat(), lastLapDistMeters, lapDistMeters, lastEstTime, estTime)
    }

    lastEstTime = estTime
    lastLapDistMeters = lapDistMeters
    lastLapDistPct = lapDistPct
    lastTotalDistancePct = totalDistancePct
    mutableSyntheticFields.carIdxDriverCarClassEstTime =
      dataSnapshot.carIdxLapDistPctList.map(this::getEstTimeForDistPct)
  }

  fun getEstTimeForDistPct(distPct: Float): Float {
    if (distPct < 0) return 0.0F
    val distMeters = trackLengthMeters * distPct
    val low = distMeters.toInt() % trackLengthMeters
    val high = (low + 1) % trackLengthMeters
    val lowEstTime = driverCarEstTimeAtDistance[low]
    val highEstTime = driverCarEstTimeAtDistance[high]
    // In the event that we cross into a new lap, we need to properly interpolate
    val highX = if (high > low) high else low + 1
    return interpolate(distMeters, low.toFloat(), highX.toFloat(), lowEstTime, highEstTime)
  }

  /** Range of ints [lastDistance, newDistance) */
  private fun intsBetween(lastDistance: Float, newDistance: Float): List<Int> {
    val start = lastDistance.toInt()
    val end = newDistance.toInt()
    if (start > end) {
      // We didn't cross any integers.
      return emptyList()
    }
    return (start..<end).toList()
  }
}