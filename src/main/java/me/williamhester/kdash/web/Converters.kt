package me.williamhester.kdash.web

import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.monitors.DriverCarLapMonitor.LogEntry

internal fun LogEntry.toLapEntry(): LapEntry {
  val log = this
  return LapEntry.newBuilder().apply {
    lapNum = log.lapNum
    driverName = log.driverName
    position = log.position
    lapTime = log.lapTime
    gapToLeader = log.gapToLeader
    fuelRemaining = log.fuelRemaining
    fuelUsed = log.fuelUsed
    trackTemp = log.trackTemp
    driverIncidents = log.driverIncidents
    teamIncidents = log.teamIncidents
    optionalRepairsRemaining = log.optionalRepairsRemaining
    repairsRemaining = log.repairsRemaining
    pitIn = log.pitIn
    pitOut = log.pitOut
    pitTime = log.pitTime
  }.build()
}
