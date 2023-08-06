package me.williamhester.kdash.web

import com.google.common.util.concurrent.RateLimiter
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.Gaps
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryServiceGrpc.LiveTelemetryServiceImplBase
import me.williamhester.kdash.monitors.DriverCarLapMonitor
import me.williamhester.kdash.monitors.RelativeMonitor
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

class LiveTelemetryService : LiveTelemetryServiceImplBase() {

  private lateinit var relativeMonitor: RelativeMonitor
  private lateinit var lapMonitor: DriverCarLapMonitor
  private val lapEntryStreamObservers = CopyOnWriteArrayList<LapEntryStreamObserverProgressHolder>()
  private val gapsStreamObservers = CopyOnWriteArrayList<GapsStreamObserverRateLimitHolder>()
  private val initializedLock = CountDownLatch(1)

  fun start(executor: Executor) {
    executor.execute(this::monitor)
    executor.execute(this::emitLoop)
  }

  private fun monitor() {
    val iRacingDataReader = IRacingLoggedDataReader(Paths.get("/Users/williamhester/Downloads/livedata.ibt"))
    relativeMonitor = RelativeMonitor(iRacingDataReader.headers)
    lapMonitor = DriverCarLapMonitor(iRacingDataReader, relativeMonitor)
    initializedLock.countDown()

    val rateLimiter = RateLimiter.create(600.0)
    for (varBuf in iRacingDataReader) {
      relativeMonitor.process(varBuf)
      lapMonitor.process(varBuf)
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
}
