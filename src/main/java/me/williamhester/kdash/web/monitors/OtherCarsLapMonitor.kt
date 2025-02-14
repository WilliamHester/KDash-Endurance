package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.api.IRacingDataReader
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import java.util.concurrent.ConcurrentHashMap

class OtherCarsLapMonitor(
  private val reader: IRacingDataReader,
  private val relativeMonitor: RelativeMonitor,
) {

  private val _logEntries = mutableListOf<LogEntry>()
  val logEntries: List<LogEntry> = _logEntries

  private val carIdxStates = ConcurrentHashMap<Int, CarState>()

  private class CarState {
    var lapNum = -1
    var driverName = "unknown"
    var position = -1
    var lapStartTime = 0.0
    var lapTime = -1.0
    var trackTemp = 0.0F
    var pitIn = false
    var pitOut = false
    var pitTime = 0.0
    var pitStartTime = 0.0

    var previousInPits = false
    var wasInPitBox = false
  }

  fun process(dataSnapshot: DataSnapshot) {
    val numDrivers = dataSnapshot.carIdxLapCompletedCount

    for (carIdx in 0 until numDrivers) {
      val carState = carIdxStates.computeIfAbsent(carIdx) { CarState() }
      carState.apply {
        val currentLap = dataSnapshot.getCarIdxLap(carIdx)
        // Check that currentLap > lapNum in case we tow. Tows actually go back to lap 0 temporarily.
        if (currentLap != lapNum && currentLap > lapNum) {
          val sessionTime = dataSnapshot.sessionTime
          // Values that are accurate at the end of the previous lap
          position = dataSnapshot.getCarIdxPosition(carIdx)
          lapTime = sessionTime - lapStartTime
          trackTemp = dataSnapshot.trackTempCrew
          driverName = reader.metadata["DriverInfo"]["Drivers"][carIdx]["UserName"].value

          val gaps = relativeMonitor.getGaps()
          val gapToLeader = if (gaps.size > carIdx) relativeMonitor.getGaps()[carIdx].gap else 0.0

          // Log the previous lap
          val newEntry =
            LogEntry(
              carId = carIdx,
              lapNum = lapNum,
              driverName = driverName,
              position = position,
              lapTime = lapTime,
              gapToLeader = gapToLeader,
              trackTemp = trackTemp,
              pitIn = pitIn,
              pitOut = pitOut,
              pitTime = pitTime,
            )
          // Ignore laps before the start of the race
          if (lapNum > 0) _logEntries.add(newEntry)

          // Values that are only accurate at the start of the new lap
          lapNum = currentLap
          lapStartTime = sessionTime

          pitTime = 0.0
          pitIn = false
          pitOut = false
        }

        val inPits = dataSnapshot.getCarIdxOnPitRoad(carIdx)

        pitIn = pitIn || (!previousInPits && inPits)
        pitOut = pitOut || (previousInPits && !inPits)

        val trackLocFlags = dataSnapshot.getCarIdxTrackSurface(carIdx)
        val isInPitBox = trackLocFlags == 1

        if (!wasInPitBox && isInPitBox) {
          pitStartTime = dataSnapshot.sessionTime
        } else if (wasInPitBox && !isInPitBox) {
          pitTime = dataSnapshot.sessionTime - pitStartTime
        }

        wasInPitBox = isInPitBox
        previousInPits = inPits
      }
    }
  }

  data class LogEntry(
    val carId: Int,
    val lapNum: Int,
    val driverName: String,
    val position: Int,
    val lapTime: Double,
    val gapToLeader: Double,
    val trackTemp: Float,
    val pitIn: Boolean,
    val pitOut: Boolean,
    val pitTime: Double,
  )
}