package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.syntheticFields
import me.williamhester.kdash.enduranceweb.proto.telemetryDataPoint
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.state.MetadataHolder
import me.williamhester.kdash.web.store.SessionStore
import kotlin.math.max
import kotlin.math.min

class DataSnapshotLogger(
  private val metadataHolder: MetadataHolder,
  private val sessionStore: SessionStore,
) {
  private val driverCarIdx = metadataHolder.metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
  private val trackLengthMeters =
    metadataHolder.metadata["WeekendInfo"]["TrackLength"].value.substringBefore(" km").toDouble() * 1000

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
      metadataHolder.metadata["WeekendInfo"]["TrackPrecipitation"].value.substringBefore(" %", "0").toDouble()

    sessionStore.insertTelemetryData(
      sessionTime,
      driverDistance,
      telemetryDataPoint {
        this.dataSnapshot = dataSnapshot
        this.syntheticFields = mutableSyntheticFields.toSyntheticFields()
      },
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

  class MutableSyntheticFields(
    var lastPitLap: Int = 0,
    var estSpeed: Float = 0.0F,
    var trackPrecip: Double = 0.0,
  ) {
    fun toSyntheticFields() = syntheticFields {
      lastPitLap = this@MutableSyntheticFields.lastPitLap
      estSpeed = this@MutableSyntheticFields.estSpeed
      trackPrecip = this@MutableSyntheticFields.trackPrecip
    }
  }
}
