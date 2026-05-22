package me.williamhester.kdash.web.service.telemetry

import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListeningExecutorService
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.LookupTables
import me.williamhester.kdash.enduranceweb.proto.SessionInfo
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.enduranceweb.proto.carClass
import me.williamhester.kdash.enduranceweb.proto.driver
import me.williamhester.kdash.enduranceweb.proto.sessionInfo
import me.williamhester.kdash.web.common.SynchronizedStreamObserver
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.store.Store

class MonitorSessionInfoHandler(
  request: ConnectRequest,
  streamObserver: StreamObserver<SessionInfo>,
  private val threadPool: ListeningExecutorService,
) : Runnable {
  private val streamObserver = SynchronizedStreamObserver(streamObserver)
  private val sessionKey = with(request.sessionIdentifier) {
    SessionKey(sessionId, subSessionId, simSessionNumber, carNumber)
  }

  override fun run() {
    val future1 = threadPool.submit { Store.getMetadataForSession(sessionKey, this::onNext) }
    val future2 = threadPool.submit { Store.monitorLookupTables(sessionKey, this::onNext) }

    val allFutures = Futures.allAsList(future1, future2)
    try {
      allFutures.get()
    } catch (e: InterruptedException) {
      allFutures.cancel(true)
    }
  }

  private fun onNext(value: SessionMetadata?) {
    if (value == null) {
      logger.atWarning().log("Metadata not found for session %s", sessionKey)
      return
    }

    val paceCarIdx = value["DriverInfo"]["PaceCarIdx"].value.toInt()
    val driverList = value["DriverInfo"]["Drivers"].listList.map {
      driver {
        carId = it["CarIdx"].value.toInt()
        carNumber = it["CarNumberRaw"].value.toInt()
        carClassId = it["CarClassID"].value.toInt()
        carClassName = it["CarClassShortName"].value
        driverName = it["UserName"].value
        teamName = it["TeamName"].value
      }
    }.filter { it.carId != paceCarIdx }

    val carClasses =
      value["DriverInfo"]["Drivers"].listList
        .map {
          carClass {
            carClassId = it["CarClassID"].value.toInt()
            carClassShortName = it["CarClassShortName"].value
            carClassColor = '#' + it["CarClassColor"].value.substringAfter('x')
          }
        }
        .toSet()

    streamObserver.onNext(
      sessionInfo {
        drivers += driverList
        driverCarEstLapTime = value["DriverInfo"]["DriverCarEstLapTime"].value.toFloat()
        this.carClasses += carClasses
      }
    )
  }

  private fun onNext(value: LookupTables?) {
    if (value == null) {
      logger.atWarning().log("LookupTables not found for session %s", sessionKey)
      return
    }

    streamObserver.onNext(
      sessionInfo {
        this.lookupTables = value
      }
    )
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}
