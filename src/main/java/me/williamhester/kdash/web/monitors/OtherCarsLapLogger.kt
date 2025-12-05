package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.state.MetadataHolder
import me.williamhester.kdash.web.store.SessionStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class OtherCarsLapLogger(
  private val metadataHolder: MetadataHolder,
  private val relativeMonitor: RelativeMonitor,
  private val sessionStore: SessionStore,
) {
  private val carIdxStates = ConcurrentHashMap<Int, CarState>()

  private inner class CarState(private val carIdx: Int) {
    var lapNum = -1
    var driverName = "unknown"
    var position = -1
    var lapStartTime = 0.0
    var lapTime = -1.0
    var trackTemp = 0.0F
    var pitIn = false
    var pitOut = false
    var pitInLap = 0
    var pitOutLap = 0
    var pitTime = 0.0
    var pitStartTime = 0.0
    var stintStartTime = 0.0

    var wasOnPitRoad: Boolean? = null
    var wasInPitBox: Boolean? = null

    private val stintLaps = mutableListOf<LogEntry>()

    fun onStintFinished(dataSnapshot: DataSnapshot) {
      val nonPitLaps = stintLaps.filterNot { it.pitOut || it.pitIn }
      val nonPitLapsTime = if (nonPitLaps.isEmpty()) 0.0 else nonPitLaps.sumOf { it.lapTime } / nonPitLaps.size
      val fastestLapTime = if (nonPitLaps.isEmpty()) -1.0 else nonPitLaps.minOf { it.lapTime }

      val stintTime = dataSnapshot.sessionTime - stintStartTime
      val newStintEntry =
        OtherCarStintEntry(
          carIdx = carIdx,
          outLap = pitOutLap,
          inLap = pitInLap,
          driverName = driverName,
          totalTime = stintTime,
          lapTimes = nonPitLaps.map { it.lapTime },
          averageLapTime = nonPitLapsTime,
          fastestLapTime = fastestLapTime,
          trackTemp = (stintLaps.sumOf { it.trackTemp.toDouble() } / max(stintLaps.size, 1)).toFloat(),
        )
      sessionStore.insertOtherCarStintEntry(newStintEntry.toOtherCarStintEntry())
      stintLaps.clear()
    }

    fun onStintStarted(dataSnapshot: DataSnapshot) {
      stintStartTime = dataSnapshot.sessionTime
    }
  }

  fun process(dataSnapshot: DataSnapshot) {
    val numDrivers = dataSnapshot.carIdxLapCompletedCount

    for (carIdx in 0 until numDrivers) {
      val carState = carIdxStates.computeIfAbsent(carIdx) { CarState(it) }
      with(carState) {
        val currentLap = dataSnapshot.getCarIdxLap(carIdx)
        // Check that currentLap > lapNum in case we tow. Tows actually go back to lap 0 temporarily.
        if (currentLap > lapNum) {
          val sessionTime = dataSnapshot.sessionTime
          // Values that are accurate at the end of the previous lap
          position = dataSnapshot.getCarIdxPosition(carIdx)
          lapTime = sessionTime - lapStartTime
          trackTemp = dataSnapshot.trackTempCrew
          driverName = metadataHolder.metadata["DriverInfo"]["Drivers"][carIdx]["UserName"].value

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
          if (lapNum > 0) sessionStore.insertOtherCarLapEntry(newEntry.toOtherCarLapEntry())

          // Values that are only accurate at the start of the new lap
          lapNum = currentLap
          lapStartTime = sessionTime

          pitTime = 0.0
          pitIn = false
          pitOut = false
        }

        val isOnPitRoad = dataSnapshot.getCarIdxOnPitRoad(carIdx)

        if (wasOnPitRoad == false && isOnPitRoad) {
          pitIn = true
          pitInLap = currentLap
        }
        if (wasOnPitRoad == true && !isOnPitRoad) {
          pitOut = true
          pitOutLap = currentLap
        }

        val trackLocFlags = dataSnapshot.getCarIdxTrackSurface(carIdx)
        val isInPitBox = trackLocFlags == 1

        if (wasInPitBox == false && isInPitBox) {
          pitStartTime = dataSnapshot.sessionTime

          onStintFinished(dataSnapshot)
        } else if (wasInPitBox == true && !isInPitBox) {
          pitTime = dataSnapshot.sessionTime - pitStartTime

          onStintStarted(dataSnapshot)
        }

        wasInPitBox = isInPitBox
        wasOnPitRoad = isOnPitRoad
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

  data class OtherCarStintEntry(
    val carIdx: Int,
    /** The out lap number. */
    val outLap: Int,
    /** The in lap number. */
    val inLap: Int,
    /** The driver name that finished the stint. */
    val driverName: String,
    /** The total time of the stint, from the time it exits the pit box to the next time it exits the pit box. */
    val totalTime: Double,
    /** A list of all lap times, from the start lap to the end lap. */
    val lapTimes: List<Double>,
    /** Average lap time excludes in and out laps. */
    val averageLapTime: Double,
    /** The fastest lap of the stint. */
    val fastestLapTime: Double,
    /** Average track temperature, in Celsius. */
    val trackTemp: Float,
  )
}