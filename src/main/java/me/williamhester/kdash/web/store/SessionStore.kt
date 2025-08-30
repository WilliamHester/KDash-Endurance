package me.williamhester.kdash.web.store

import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.enduranceweb.proto.StintEntry
import me.williamhester.kdash.enduranceweb.proto.TelemetryDataPoint
import me.williamhester.kdash.web.models.SessionKey

class SessionStore(private val sessionKey: SessionKey) {
  fun insertTelemetryData(sessionTime: Double, driverDistance: Float, telemetryDataPoint: TelemetryDataPoint) {
    Store.insertTelemetryData(sessionKey, sessionTime, driverDistance, telemetryDataPoint)
  }
  fun insertLapEntry(lapEntry: LapEntry) = Store.insertLapEntry(sessionKey, lapEntry)
  fun insertStintEntry(stintEntry: StintEntry) = Store.insertStintEntry(sessionKey, stintEntry)
  fun insertOtherCarLapEntry(lapEntry: OtherCarLapEntry) = Store.insertOtherCarLapEntry(sessionKey, lapEntry)
}
