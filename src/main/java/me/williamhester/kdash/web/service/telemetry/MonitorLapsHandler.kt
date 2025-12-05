package me.williamhester.kdash.web.service.telemetry

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningExecutorService
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.LapData
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.enduranceweb.proto.OtherCarStintEntry
import me.williamhester.kdash.enduranceweb.proto.StintEntry
import me.williamhester.kdash.enduranceweb.proto.lapData
import me.williamhester.kdash.web.common.SynchronizedStreamObserver
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.store.Store
import me.williamhester.kdash.web.store.StreamedResponseListener
import kotlin.reflect.KClass

internal class MonitorLapsHandler(
  request: ConnectRequest,
  responseObserver: StreamObserver<LapData>,
  private val threadPool: ListeningExecutorService,
) : Runnable, StreamedResponseListener<Any> {
  private val responseObserver = SynchronizedStreamObserver(responseObserver)
  private val sessionKey = with(request.sessionIdentifier) {
    SessionKey(sessionId, subSessionId, simSessionNumber, carNumber)
  }

  override fun run() {
    val future1 = threadPool.submit { Store.getDriverLaps(sessionKey, this) }
    val future2 = threadPool.submit { Store.getDriverStints(sessionKey, this) }
    val future3 = threadPool.submit { Store.getOtherCarLaps(sessionKey, this) }
    val future4 = threadPool.submit { Store.getOtherCarStints(sessionKey, this) }

    val allFutures = Futures.allAsList(future1, future2, future3, future4)
    try {
      allFutures.get()
    } catch (e: InterruptedException) {
      allFutures.cancel(true)
    }
  }

  override fun onNext(value: Any) {
    val lapData = lapData {
      when (value) {
        is LapEntry -> driverLap = value
        is StintEntry -> driverStint = value
        is OtherCarLapEntry -> otherCarLap = value
        is OtherCarStintEntry -> otherCarStint = value
        else -> throw UnknownDataTypeException(value::class)
      }
    }
    responseObserver.onNext(lapData)
  }

  private class UnknownDataTypeException(type: KClass<*>) : Exception("Unknown type: $type")
}