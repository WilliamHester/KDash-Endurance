package me.williamhester.kdash.web.service.telemetrypusher

import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionKey

/** Convert a [SessionMetadata] to a [SessionKey]. */
internal fun SessionMetadata.toSessionKey(): SessionKey {
  val sessionId = this["WeekendInfo"]["SessionID"].value.toInt()
  val subSessionId = this["WeekendInfo"]["SubSessionID"].value.toInt()
  val simSessionNumber = this["SessionInfo"]["SimSessionNumber"].value.ifBlank { "0" }.toInt()
  val driverCarIdx = this["DriverInfo"]["DriverCarIdx"].value.toInt()
  // DriverInfo:Drivers:idx:CarNumber and ...:CarNumberRaw both exist. However, CarNumberRaw is sometimes over 1000
  // and it's not clear why. Removing the quotes from CarNumber seems to more accurately get what's displayed.
  val carNumber =
    this["DriverInfo"]["Drivers"][driverCarIdx]["CarNumber"]
      .value
      .substringAfter('"')
      .substringBefore('"')
  return SessionKey(
    sessionId = sessionId,
    subSessionId = subSessionId,
    sessionNum = simSessionNumber,
    carNumber = carNumber,
  )
}
