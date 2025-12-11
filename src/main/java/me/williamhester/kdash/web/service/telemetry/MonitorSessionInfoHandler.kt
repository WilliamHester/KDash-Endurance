package me.williamhester.kdash.web.service.telemetry

import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.SessionInfo
import me.williamhester.kdash.enduranceweb.proto.driver
import me.williamhester.kdash.enduranceweb.proto.sessionInfo
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.store.Store

class MonitorSessionInfoHandler(
  request: ConnectRequest,
  private val streamObserver: StreamObserver<SessionInfo>,
) : Runnable {
  private val sessionKey = with(request.sessionIdentifier) {
    SessionKey(sessionId, subSessionId, simSessionNumber, carNumber)
  }

  override fun run() {
    // TODO: Make this null safe
    val metadata = Store.getMetadataForSession(sessionKey)!!

    val paceCarIdx = metadata["DriverInfo"]["PaceCarIdx"].value.toInt()
    val driverList = metadata["DriverInfo"]["Drivers"].listList.map {
      driver {
        carId = it["CarIdx"].value.toInt()
        carNumber = it["CarNumberRaw"].value.toInt()
        carClassId = it["CarClassID"].value.toInt()
        carClassName = it["CarClassShortName"].value
        driverName = it["UserName"].value
        teamName = it["TeamName"].value
      }
    }.filter { it.carId != paceCarIdx }

    streamObserver.onNext(
      sessionInfo {
        driverCarIdx = metadata["DriverInfo"]["DriverCarIdx"].value.toInt()
        driverCarEstLapTime = metadata["DriverInfo"]["DriverCarEstLapTime"].value.toFloat()
        drivers.addAll(driverList)
      }
    )
  }
}
