package me.williamhester.kdash.web.service.telemetry

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningExecutorService
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.LapData
import me.williamhester.kdash.enduranceweb.proto.lapData
import me.williamhester.kdash.web.common.SynchronizedStreamObserver
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.store.Store

internal class MonitorLapsHandler(
  responseObserver: StreamObserver<LapData>,
  private val threadPool: ListeningExecutorService,
) : Runnable {
  private val responseObserver = SynchronizedStreamObserver(responseObserver)
  private val sessionKey = SessionKey(0, 0, 0, "64")

  override fun run() {
    val future1 = threadPool.submit {
      Store.getDriverLaps(sessionKey) {
        responseObserver.onNext(
          lapData {
            driverLap = it
          }
        )
      }
    }
    val future2 = threadPool.submit {
      Store.getDriverStints(sessionKey) {
        responseObserver.onNext(
          lapData {
            driverStint = it
          }
        )
      }
    }
    val future3 = threadPool.submit {
      Store.getOtherCarLaps(sessionKey) {
        responseObserver.onNext(
          lapData {
            otherCarLap = it
          }
        )
      }
    }

    // Block until all futures are done.
    Futures.allAsList(future1, future2, future3).get()
  }
}