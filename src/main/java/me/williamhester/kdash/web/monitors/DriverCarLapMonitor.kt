package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.api.IRacingDataReader
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import kotlin.math.max
import kotlin.math.min

class DriverCarLapMonitor(
  private val reader: IRacingDataReader,
  private val relativeMonitor: RelativeMonitor,
) {

  private val _logEntries = mutableListOf<LogEntry>()
  val logEntries: List<LogEntry> = _logEntries

  private var driverCarIdx = -1

  private var lapNum = -1
  private var driverName = "unknown"
  private var position = -1
  private var lapStartTime = 0.0
  private var lapTime = -1.0
  private var fuelRemaining = 0.0F
  private var trackTemp = 0.0F
  private var driverIncidents = 0
  private var teamIncidents = 0
  private var optionalRepairsRemaining = 0.0F
  private var repairsRemaining = 0.0F
  private var pitIn = false
  private var pitOut = false
  private var pitTime = 0.0
  private var pitStartTime = 0.0
  private var maxSpeed = 0.0F

  private var previousInPits = false
  private var wasInPitBox = false
  private var didAddFuel = false
  private var fuelUsedBeforeRefuel = 0.0F
  private var minFuelRemaining = 1000.0F

  fun process(dataSnapshot: DataSnapshot) {
    if (driverCarIdx == -1) {
      driverCarIdx = reader.metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
    }

    val currentLap = dataSnapshot.lap
    val fuelRemaining = dataSnapshot.fuelLevel
    // Check that currentLap > lapNum in case we tow. Tows actually go back to lap 0 temporarily.
    if (currentLap != lapNum && currentLap > lapNum) {
      val sessionTime = dataSnapshot.sessionTime
      // Values that are accurate at the end of the previous lap
      position = dataSnapshot.playerCarPosition
      lapTime = sessionTime - lapStartTime
      trackTemp = dataSnapshot.trackTempCrew
      // TODO: Find a way to reliably get the current driver name.
      driverName = reader.metadata["DriverInfo"]["Drivers"][driverCarIdx]["UserName"].value
      driverIncidents = dataSnapshot.driverIncidentCount
      teamIncidents = dataSnapshot.teamIncidentCount
      val fuelUsed = (this.fuelRemaining - fuelRemaining) + fuelUsedBeforeRefuel
      this.fuelRemaining = fuelRemaining

      val gapToLeader = if (driverCarIdx < relativeMonitor.getGaps().size) {
        relativeMonitor.getGaps()[driverCarIdx].gap
      } else {
        0.0
      }

      // Log the previous lap
      val newEntry =
        LogEntry(
          lapNum = lapNum,
          driverName = driverName,
          position = position,
          lapTime = lapTime,
          gapToLeader = gapToLeader,
          fuelRemaining = fuelRemaining,
          fuelUsed = fuelUsed,
          trackTemp = trackTemp,
          driverIncidents = driverIncidents,
          teamIncidents = teamIncidents,
          optionalRepairsRemaining = optionalRepairsRemaining,
          repairsRemaining = repairsRemaining,
          pitIn = pitIn,
          pitOut = pitOut,
          pitTime = pitTime,
          maxSpeed = maxSpeed,
        )
      // Ignore laps before the start of the race
      if (lapNum > 0) _logEntries.add(newEntry)

      // Values that are only accurate at the start of the new lap
      lapNum = currentLap
      lapStartTime = sessionTime

      pitTime = 0.0
      pitIn = false
      pitOut = false

      fuelUsedBeforeRefuel = 0.0F
      minFuelRemaining = fuelRemaining
      didAddFuel = false

      maxSpeed = 0.0F
    }

    minFuelRemaining = min(fuelRemaining, minFuelRemaining)
    if (fuelRemaining > minFuelRemaining) {
      if (!didAddFuel) {
        fuelUsedBeforeRefuel = this.fuelRemaining - minFuelRemaining
        didAddFuel = true
      }
      this.fuelRemaining = fuelRemaining
    }

    val inPits = dataSnapshot.onPitRoad

    pitIn = pitIn || (!previousInPits && inPits)
    pitOut = pitOut || (previousInPits && !inPits)

    val trackLocFlags = dataSnapshot.carIdxTrackSurfaceList[driverCarIdx]
    val isInPitBox = trackLocFlags == 1

    if (!wasInPitBox && isInPitBox) {
      pitStartTime = dataSnapshot.sessionTime
    } else if (wasInPitBox && !isInPitBox) {
      pitTime = dataSnapshot.sessionTime - pitStartTime
    }
    if (dataSnapshot.pitstopActive) {
      optionalRepairsRemaining = dataSnapshot.pitOptRepairLeft
      repairsRemaining = dataSnapshot.pitRepairLeft
    }

    wasInPitBox = isInPitBox
    previousInPits = inPits

    maxSpeed = max(maxSpeed, dataSnapshot.speed)
  }

  data class LogEntry(
    val lapNum: Int,
    val driverName: String,
    val position: Int,
    val lapTime: Double,
    val gapToLeader: Double,
    val fuelRemaining: Float,
    val fuelUsed: Float,
    val trackTemp: Float,
    val driverIncidents: Int,
    val teamIncidents: Int,
    val optionalRepairsRemaining: Float,
    val repairsRemaining: Float,
    val pitIn: Boolean,
    val pitOut: Boolean,
    val pitTime: Double,
    val maxSpeed: Float,
  )
}