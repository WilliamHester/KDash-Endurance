package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.MutableSyntheticFields
import me.williamhester.kdash.web.models.TelemetryDataPoint
import me.williamhester.kdash.web.state.MetadataFetcher
import kotlin.math.max
import kotlin.math.min

class DataSnapshotMonitor(
  private val metadataFetcher: MetadataFetcher,
) {
  private val _telemetryDataPoints = mutableListOf<TelemetryDataPoint>()
  val telemetryDataPoints: List<TelemetryDataPoint> = _telemetryDataPoints

  private val driverCarIdx = metadataFetcher.metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
  private val trackLengthMeters =
    metadataFetcher.metadata["WeekendInfo"]["TrackLength"].value.substringBefore(" km").toDouble() * 1000

  private var lastLapDistMeters = 0.0
  private var lastSessionTime = 0.0

  private val mutableSyntheticFields = MutableSyntheticFields()

  fun process(dataSnapshot: DataSnapshot) {
    val sessionTime = dataSnapshot.sessionTime
    val driverDistance = dataSnapshot.lap + dataSnapshot.getCarIdxLapDistPct(driverCarIdx)

    if (dataSnapshot.onPitRoad) {
      mutableSyntheticFields.lastPitLap = dataSnapshot.lap
    }
    estimateSpeed(dataSnapshot, sessionTime)
    mutableSyntheticFields.trackPrecip =
      metadataFetcher.metadata["WeekendInfo"]["TrackPrecipitation"].value.substringBefore(" %", "0").toDouble()

    _telemetryDataPoints.add(
      TelemetryDataPoint(sessionTime, driverDistance, dataSnapshot, mutableSyntheticFields.toSyntheticFields())
    )
  }

  private fun estimateSpeed(
    dataSnapshot: DataSnapshot,
    sessionTime: Double
  ) {
    val lapDistanceMeters = trackLengthMeters * dataSnapshot.carIdxLapDistPctList[driverCarIdx]
    val distTravelled =
      if (lapDistanceMeters < lastLapDistMeters) {
        lapDistanceMeters + (trackLengthMeters - lastLapDistMeters)
      } else {
        lapDistanceMeters - lastLapDistMeters
      }
    val currentSessionTime = dataSnapshot.sessionTime
    val estSpeed = (distTravelled / (currentSessionTime - lastSessionTime)).toFloat()
    // It's possible that we nearly divide by zero or get some other wacky result if someone cuts the course.
    // Some reasonable min/max values are 134 m/s (300 mph) and -100 m/s (-223 mph)
    val sanitizedEstSpeed = max(min(estSpeed, 134.0F), -100.0F)
    mutableSyntheticFields.estSpeed = sanitizedEstSpeed

    lastLapDistMeters = lapDistanceMeters
    lastSessionTime = sessionTime
  }
}
