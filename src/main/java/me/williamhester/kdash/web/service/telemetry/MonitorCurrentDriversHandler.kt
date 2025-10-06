package me.williamhester.kdash.web.service.telemetry

import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.CurrentDrivers
import me.williamhester.kdash.enduranceweb.proto.currentDrivers
import me.williamhester.kdash.enduranceweb.proto.driver
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.store.Store

class MonitorCurrentDriversHandler(
  request: ConnectRequest,
  private val streamObserver: StreamObserver<CurrentDrivers>,
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
      currentDrivers {
        drivers.addAll(driverList)
      }
    )
  }
}
