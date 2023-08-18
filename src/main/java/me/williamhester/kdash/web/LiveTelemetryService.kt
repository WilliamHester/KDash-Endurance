package me.williamhester.kdash.web

import com.google.common.util.concurrent.RateLimiter
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.CurrentDrivers
import me.williamhester.kdash.enduranceweb.proto.Driver
import me.williamhester.kdash.enduranceweb.proto.DriverDistances
import me.williamhester.kdash.enduranceweb.proto.Gaps
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryServiceGrpc.LiveTelemetryServiceImplBase
import me.williamhester.kdash.monitors.DriverCarLapMonitor
import me.williamhester.kdash.monitors.DriverDistancesMonitor
import me.williamhester.kdash.monitors.DriverMonitor
import me.williamhester.kdash.monitors.RelativeMonitor
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

class LiveTelemetryService : LiveTelemetryServiceImplBase() {
  private val iRacingDataReader = IRacingLoggedDataReader(Paths.get("/Users/williamhester/Downloads/livedata.ibt"))

  private lateinit var relativeMonitor: RelativeMonitor
  private lateinit var lapMonitor: DriverCarLapMonitor
  private lateinit var driverDistancesMonitor: DriverDistancesMonitor
  private val driverMonitor = DriverMonitor(iRacingDataReader)

  private val lapEntryStreamObservers = CopyOnWriteArrayList<LapEntryStreamObserverProgressHolder>()
  private val gapsStreamObservers = CopyOnWriteArrayList<GapsStreamObserverRateLimitHolder>()
  private val currentDriversStreamObservers = CopyOnWriteArrayList<CurrentDriversStreamObserverHolder>()
  private val driverDistancesStreamObserverHolders = CopyOnWriteArrayList<DriverDistanceStreamObserverHolder>()
  private val initializedLock = CountDownLatch(1)

  fun start(executor: Executor) {
    executor.execute(this::monitor)
    executor.execute(this::emitLoop)
  }

  private fun monitor() {
    relativeMonitor = RelativeMonitor(iRacingDataReader.headers)
    lapMonitor = DriverCarLapMonitor(iRacingDataReader, relativeMonitor)
    driverDistancesMonitor = DriverDistancesMonitor(iRacingDataReader)
    initializedLock.countDown()

    val rateLimiter = RateLimiter.create(600.0)
    for (varBuf in iRacingDataReader) {
      relativeMonitor.process(varBuf)
      lapMonitor.process(varBuf)
      driverDistancesMonitor.process(varBuf)
      rateLimiter.acquire()
    }
  }

  private fun emitLoop() {
    initializedLock.await()
    while (true) {
      emitAll()
    }
  }

  private fun emitAll() {
    emitDriverLapLogs()
    emitAllGapsRateLimited()
    emitNewDriversPerStream()
    emitDriverDistances()
  }

  private fun emitDriverLapLogs() {
    val lapEntries = lapMonitor.logEntries

    val responseObserversToRemove = mutableSetOf<LapEntryStreamObserverProgressHolder>()
    for (responseObserverHolder in lapEntryStreamObservers) {
      val lapsSent = responseObserverHolder.lapsSent
      while (lapEntries.size > lapsSent.get()) {
        try {
          responseObserverHolder.responseObserver.onNext(lapEntries[lapsSent.getAndIncrement()].toLapEntry())
        } catch (e: StatusRuntimeException) {
          responseObserversToRemove += responseObserverHolder
          break
        }
      }
    }
    lapEntryStreamObservers.removeAll(responseObserversToRemove)
  }

  private fun emitAllGapsRateLimited() {
    val gapResponseObserversToRemove = mutableSetOf<GapsStreamObserverRateLimitHolder>()
    for (responseObserverHolder in gapsStreamObservers) {
      val rateLimiter = responseObserverHolder.rateLimiter
      if (rateLimiter.tryAcquire()) {
        try {
          responseObserverHolder.responseObserver.onNext(
            Gaps.newBuilder().addAllGaps(relativeMonitor.getGaps().map(RelativeMonitor.GapToCarId::toGap)).build()
          )
        } catch (e: StatusRuntimeException) {
          gapResponseObserversToRemove += responseObserverHolder
          break
        }
      }
    }
    gapsStreamObservers.removeAll(gapResponseObserversToRemove)
  }

  private fun emitNewDriversPerStream() {
    val driverObserversToRemove = mutableSetOf<CurrentDriversStreamObserverHolder>()
    val currentDrivers = driverMonitor.currentDrivers.map {
      Driver.newBuilder().setCarId(it.key).setCarNumber(it.value.carNumber).setDriverName(it.value.driverName).build()
    }
    for (responseObserverHolder in currentDriversStreamObservers) {
      val previousDrivers = responseObserverHolder.previousDrivers
      if (currentDrivers != previousDrivers) {
        try {
          responseObserverHolder.responseObserver.onNext(
            CurrentDrivers.newBuilder().addAllDrivers(currentDrivers).build()
          )
          responseObserverHolder.previousDrivers = currentDrivers
        } catch (e: StatusRuntimeException) {
          driverObserversToRemove += responseObserverHolder
          break
        }
      }
    }
    currentDriversStreamObservers.removeAll(driverObserversToRemove)
  }

  private fun emitDriverDistances() {
    val lapEntries = driverDistancesMonitor.distances

    val responseObserversToRemove = mutableSetOf<DriverDistanceStreamObserverHolder>()
    for (responseObserverHolder in driverDistancesStreamObserverHolders) {
      val ticksSent = responseObserverHolder.lastDriverDistance
      while (ticksSent.get() < lapEntries.size) {
        try {
          responseObserverHolder.responseObserver.onNext(
            lapEntries[ticksSent.getAndIncrement()].toDriverDistances()
          )
        } catch (e: StatusRuntimeException) {
          responseObserversToRemove += responseObserverHolder
          break
        }
      }
    }
    driverDistancesStreamObserverHolders.removeAll(responseObserversToRemove)
  }

  override fun monitorDriverLaps(
    request: ConnectRequest,
    responseObserver: StreamObserver<LapEntry>
  ) {
    lapEntryStreamObservers.add(LapEntryStreamObserverProgressHolder(responseObserver))
  }

  private class LapEntryStreamObserverProgressHolder(
    val responseObserver: StreamObserver<LapEntry>,
  ) {
    val lapsSent = AtomicInteger(0)
  }

  override fun monitorCurrentGaps(
    request: ConnectRequest,
    responseObserver: StreamObserver<Gaps>
  ) {
    gapsStreamObservers.add(GapsStreamObserverRateLimitHolder(responseObserver))
  }

  private class GapsStreamObserverRateLimitHolder(
    val responseObserver: StreamObserver<Gaps>,
  ) {
    val rateLimiter: RateLimiter = RateLimiter.create(5.0)
  }

  override fun monitorCurrentDrivers(
    request: ConnectRequest,
    responseObserver: StreamObserver<CurrentDrivers>
  ) {
    currentDriversStreamObservers.add(CurrentDriversStreamObserverHolder(responseObserver))
  }

  private class CurrentDriversStreamObserverHolder(
    val responseObserver: StreamObserver<CurrentDrivers>,
  ) {
    var previousDrivers = listOf<Driver>()
  }

  override fun monitorDriverDistances(
    request: ConnectRequest,
    responseObserver: StreamObserver<DriverDistances>
  ) {
    driverDistancesStreamObserverHolders.add(DriverDistanceStreamObserverHolder(responseObserver))
  }

  private class DriverDistanceStreamObserverHolder(
    val responseObserver: StreamObserver<DriverDistances>,
  ) {
    var lastDriverDistance = AtomicInteger(0)
  }
}
