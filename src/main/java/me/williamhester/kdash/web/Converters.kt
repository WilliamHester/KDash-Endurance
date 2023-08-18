package me.williamhester.kdash.web

import me.williamhester.kdash.enduranceweb.proto.DriverDistance
import me.williamhester.kdash.enduranceweb.proto.DriverDistances
import me.williamhester.kdash.enduranceweb.proto.Gap
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.monitors.DriverCarLapMonitor
import me.williamhester.kdash.monitors.DriverDistancesMonitor
import me.williamhester.kdash.monitors.OtherCarsLapMonitor
import me.williamhester.kdash.monitors.RelativeMonitor.GapToCarId

internal fun DriverCarLapMonitor.LogEntry.toLapEntry(): LapEntry {
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

internal fun OtherCarsLapMonitor.LogEntry.toOtherCarLapEntry(): OtherCarLapEntry {
  val log = this
  return OtherCarLapEntry.newBuilder().apply {
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

internal fun GapToCarId.toGap(): Gap {
  val gapToCarId = this
  return Gap.newBuilder().apply {
    carId = gapToCarId.carId
    gap = gapToCarId.gap
  }.build()
}

internal fun DriverDistancesMonitor.DriverDistances.toDriverDistances(): DriverDistances {
  val driverDistances = this
  return DriverDistances.newBuilder().apply {
    sessionTime = driverDistances.sessionTime.toFloat()
    for (distance in driverDistances.distances) {
      addDistances(DriverDistance.newBuilder().setCarId(distance.carId).setDriverDistance(distance.distance))
    }
  }.build()
}
