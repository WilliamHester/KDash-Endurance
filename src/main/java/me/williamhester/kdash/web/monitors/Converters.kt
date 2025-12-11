package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarStintEntry
import me.williamhester.kdash.enduranceweb.proto.StintEntry
import me.williamhester.kdash.enduranceweb.proto.otherCarStintEntry
import me.williamhester.kdash.enduranceweb.proto.stintEntry

fun DriverCarLapLogger.LogEntry.toLapEntry(): LapEntry {
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
    maxSpeed = log.maxSpeed
  }.build()
}

fun DriverCarLapLogger.StintEntry.toStintEntry(): StintEntry {
  val stint = this
  return stintEntry {
    outLap = stint.outLap
    inLap = stint.inLap
    driverName = stint.driverName
    totalTime = stint.totalTime
    lapTimes.addAll(stint.lapTimes)
    averageLapTime = stint.averageLapTime
    fastestLapTime = stint.fastestLapTime
    trackTemp = stint.trackTemp
    incidents = stint.incidents
  }
}

fun OtherCarsLapLogger.LogEntry.toOtherCarLapEntry(): OtherCarLapEntry {
  val log = this
  return OtherCarLapEntry.newBuilder().apply {
    carId = log.carId
    lapNum = log.lapNum
    driverName = log.driverName
    position = log.position
    lapTime = log.lapTime
    gapToLeader = log.gapToLeader
    trackTemp = log.trackTemp
    pitIn = log.pitIn
    pitOut = log.pitOut
    pitTime = log.pitTime
  }.build()
}

fun OtherCarsLapLogger.OtherCarStintEntry.toOtherCarStintEntry(): OtherCarStintEntry {
  val stint = this
  return otherCarStintEntry {
    carIdx = stint.carIdx
    outLap = stint.outLap
    inLap = stint.inLap
    driverName = stint.driverName
    totalTime = stint.totalTime
    lapTimes.addAll(stint.lapTimes)
    averageLapTime = stint.averageLapTime
    fastestLapTime = stint.fastestLapTime
    trackTemp = stint.trackTemp
  }
}
