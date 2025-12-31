package me.williamhester.kdash.web.service.telemetry

import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.StaticSessionInfo
import me.williamhester.kdash.enduranceweb.proto.carClass
import me.williamhester.kdash.enduranceweb.proto.staticSessionInfo
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.store.Store

class GetStaticSessionInfoHandler(
  request: ConnectRequest,
  private val streamObserver: StreamObserver<StaticSessionInfo>,
) : Runnable {
  private val sessionKey = with(request.sessionIdentifier) {
    SessionKey(sessionId, subSessionId, simSessionNumber, carNumber)
  }

  override fun run() {
    // TODO: Make this null safe
    val metadata = Store.getMetadataForSession(sessionKey)!!

    val carClasses =
      metadata["DriverInfo"]["Drivers"].listList
        .map {
          carClass {
            carClassId = it["CarClassID"].value.toInt()
            carClassShortName = it["CarClassShortName"].value
            carClassColor = '#' + it["CarClassColor"].value.substringAfter('x')
          }
        }
        .toSet()

    streamObserver.onNext(
      staticSessionInfo {
        driverCarIdx = metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
        driverCarEstLapTime = metadata["DriverInfo"]["DriverCarEstLapTime"].value.toFloat()
        isMulticlass = carClasses.size > 1
        this.carClasses += carClasses
        driverCarTankSize =
          metadata["DriverInfo"]["DriverCarFuelMaxLtr"].value.toFloat() *
              metadata["DriverInfo"]["DriverCarMaxFuelPct"].value.toFloat()
        val session = metadata["SessionInfo"]["Sessions"][sessionKey.sessionNum]
        val lapLimitValue = session["SessionLaps"].value
        lapLimit = if (lapLimitValue == "unlimited") -1 else lapLimitValue.toInt()
        isTeamEvent = metadata["WeekendInfo"]["TeamRacing"].value.toInt() == 1
      }
    )
  }
}
