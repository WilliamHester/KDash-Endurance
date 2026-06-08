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
import me.williamhester.kdash.enduranceweb.proto.pitOptions
import me.williamhester.kdash.enduranceweb.proto.sessionInfo
import me.williamhester.kdash.web.common.SynchronizedStreamObserver
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.models.TelemetryRange
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
  private var lastPitOptions = pitOptions {
    // Set the fuel to add to a number that will never be set in game so the first time we read the pit options, the
    // value gets sent.
    fuelToAdd = -100.0F
  }

  override fun run() {
    val future1 = threadPool.submit { Store.getMetadataForSession(sessionKey, this::onNext) }
    val future2 = threadPool.submit { Store.monitorLookupTables(sessionKey, this::onNext) }
    val future3 = threadPool.submit { monitorPitInfo() }

    val allFutures = Futures.allAsList(future1, future2, future3)
    try {
      allFutures.get()
    } catch (e: InterruptedException) {
      allFutures.cancel(true)
    }
  }

  private fun monitorPitInfo() {
    val range = Store.getSessionTelemetryRange(sessionKey) ?: TelemetryRange(0.0, 0.0, 0.0F, 0.0F)

    // Send data starting with the latest value.
    Store.getTelemetryForRange(sessionKey, range.maxSessionTime - 0.001, Double.MAX_VALUE, 60.0) {
      val pitSvFlags = it.dataSnapshot.pitSvFlags
      val newPitOptions = pitOptions {
        lfTire = pitSvFlags and 0x0001 != 0
        rfTire = pitSvFlags and 0x0002 != 0
        lrTire = pitSvFlags and 0x0004 != 0
        rrTire = pitSvFlags and 0x0008 != 0
        startFueling = pitSvFlags and 0x0010 != 0
        windshieldTearoff = pitSvFlags and 0x0020 != 0
        fastRepair = pitSvFlags and 0x0040 != 0
        fuelToAdd = it.dataSnapshot.pitSvFuel
      }
      if (newPitOptions != lastPitOptions) {
        lastPitOptions = newPitOptions
        streamObserver.onNext(sessionInfo { this.selectedPitOptions = newPitOptions })
      }
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
        carClassName = it["CarClassShortName"].value.ifBlank { "Class ID: ${it["CarClassID"].value}" }
        driverName = it["UserName"].value
        teamName = it["TeamName"].value
        estimatedLapTime = it["CarClassEstLapTime"].value.toFloat()
      }
    }.filter { it.carId != paceCarIdx }

    val carClasses =
      value["DriverInfo"]["Drivers"].listList
        .map {
          carClass {
            carClassId = it["CarClassID"].value.toInt()
            carClassShortName = it["CarClassShortName"].value.ifBlank { "Class ID: ${it["CarClassID"].value}" }
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
