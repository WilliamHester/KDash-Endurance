package me.williamhester.kdash.web

import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.RateLimiter
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.CurrentDrivers
import me.williamhester.kdash.enduranceweb.proto.DataRange
import me.williamhester.kdash.enduranceweb.proto.Driver
import me.williamhester.kdash.enduranceweb.proto.DriverDistances
import me.williamhester.kdash.enduranceweb.proto.Gaps
import me.williamhester.kdash.enduranceweb.proto.LapEntry
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryServiceGrpc.LiveTelemetryServiceImplBase
import me.williamhester.kdash.enduranceweb.proto.OtherCarLapEntry
import me.williamhester.kdash.enduranceweb.proto.QueryTelemetryRequest
import me.williamhester.kdash.enduranceweb.proto.QueryTelemetryResponse
import me.williamhester.kdash.enduranceweb.proto.TelemetryData
import me.williamhester.kdash.enduranceweb.proto.dataRange
import me.williamhester.kdash.enduranceweb.proto.queryTelemetryResponse
import me.williamhester.kdash.web.monitors.DriverCarLapMonitor
import me.williamhester.kdash.web.monitors.DriverDistancesMonitor
import me.williamhester.kdash.web.monitors.DriverMonitor
import me.williamhester.kdash.web.monitors.LiveTelemetryMonitor
import me.williamhester.kdash.web.monitors.OtherCarsLapMonitor
import me.williamhester.kdash.web.monitors.RelativeMonitor
import me.williamhester.kdash.web.state.DataSnapshotQueue
import me.williamhester.kdash.web.state.MetadataHolder
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LiveTelemetryService(
  private val metadataHolder: MetadataHolder,
  private val dataSnapshotQueue: DataSnapshotQueue,
) : LiveTelemetryServiceImplBase() {

  private lateinit var relativeMonitor: RelativeMonitor
  private lateinit var lapMonitor: DriverCarLapMonitor
  private lateinit var otherCarsLapMonitor: OtherCarsLapMonitor
  private lateinit var driverDistancesMonitor: DriverDistancesMonitor
  private lateinit var liveTelemetryMonitor: LiveTelemetryMonitor
  private val driverMonitor = DriverMonitor(metadataHolder)

  private val lapEntryStreamObservers = CopyOnWriteArrayList<LapEntryStreamObserverProgressHolder>()
  private val otherCarLapEntryStreamObservers = CopyOnWriteArrayList<OtherCarLapEntryStreamObserverProgressHolder>()
  private val gapsStreamObservers = CopyOnWriteArrayList<GapsStreamObserverRateLimitHolder>()
  private val currentDriversStreamObservers = CopyOnWriteArrayList<CurrentDriversStreamObserverHolder>()
  private val driverDistancesStreamObserverHolders = CopyOnWriteArrayList<DriverDistanceStreamObserverHolder>()
  private val telemetryDataStreamObserverHolders = CopyOnWriteArrayList<TelemetryDataStreamObserverProgressHolder>()
  private val initializedLock = CountDownLatch(1)

  fun start(executor: Executor) {
    executor.execute(this::monitor)
    executor.execute(this::emitLoop)
  }

  private fun monitor() {
    relativeMonitor = RelativeMonitor()
    lapMonitor = DriverCarLapMonitor(metadataHolder, relativeMonitor)
    otherCarsLapMonitor = OtherCarsLapMonitor(metadataHolder, relativeMonitor)
    driverDistancesMonitor = DriverDistancesMonitor()
    liveTelemetryMonitor = LiveTelemetryMonitor(metadataHolder)
    initializedLock.countDown()

    val rateLimiter = RateLimiter.create(6000.0)
    for (dataSnapshot in dataSnapshotQueue) {
      logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log("Processing new buffer.")
      relativeMonitor.process(dataSnapshot)
      lapMonitor.process(dataSnapshot)
      otherCarsLapMonitor.process(dataSnapshot)
      driverDistancesMonitor.process(dataSnapshot)
      liveTelemetryMonitor.process(dataSnapshot)
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
    emitOtherCarsLapLogs()
    emitAllGapsRateLimited()
    emitNewDriversPerStream()
    emitDriverDistances()
    emitTelemetryData()
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

  private fun emitOtherCarsLapLogs() {
    val lapEntries = otherCarsLapMonitor.logEntries

    val responseObserversToRemove = mutableSetOf<OtherCarLapEntryStreamObserverProgressHolder>()
    for (responseObserverHolder in otherCarLapEntryStreamObservers) {
      val lapsSent = responseObserverHolder.lapsSent
      while (lapEntries.size > lapsSent.get()) {
        try {
          responseObserverHolder.responseObserver.onNext(lapEntries[lapsSent.getAndIncrement()].toOtherCarLapEntry())
        } catch (e: StatusRuntimeException) {
          responseObserversToRemove += responseObserverHolder
          break
        }
      }
    }
    otherCarLapEntryStreamObservers.removeAll(responseObserversToRemove)
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
      Driver.newBuilder().apply {
        carId = it.key
        carNumber = it.value.carNumber
        carClassId = it.value.carClassId
        carClassName = it.value.carClassName
        driverName = it.value.driverName
        teamName = it.value.teamName
      }.build()
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

  private fun emitTelemetryData() {
    val telemetryData = liveTelemetryMonitor.telemetryData

    val responseObserversToRemove = mutableSetOf<TelemetryDataStreamObserverProgressHolder>()
    for (responseObserverHolder in telemetryDataStreamObserverHolders) {
      val telemetryDataSent = responseObserverHolder.entriesSent
      while (telemetryData.size > telemetryDataSent.get()) {
        val shouldRemove = try {
          responseObserverHolder.sendIfNecessary(telemetryData[telemetryDataSent.getAndIncrement()])
        } catch (e: StatusRuntimeException) {
          true
        }
        if (shouldRemove) {
          responseObserversToRemove += responseObserverHolder
          break
        }
      }
    }
    telemetryDataStreamObserverHolders.removeAll(responseObserversToRemove)
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

  override fun monitorOtherCarsLaps(request: ConnectRequest, responseObserver: StreamObserver<OtherCarLapEntry>) {
    otherCarLapEntryStreamObservers.add(OtherCarLapEntryStreamObserverProgressHolder(responseObserver))
  }

  private class OtherCarLapEntryStreamObserverProgressHolder(
    val responseObserver: StreamObserver<OtherCarLapEntry>,
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

  override fun queryTelemetry(
    request: QueryTelemetryRequest,
    responseObserver: StreamObserver<QueryTelemetryResponse>,
  ) {
    val wrappedObserver = QueryTelemetryResponseStreamObserver(responseObserver)

    val hz = if (request.sampleRateHz > 0) request.sampleRateHz else 100.0
    val startTime = if (request.minSessionTime == -1.0) {
      // -1.0 means that we should just retrieve the last DEFAULT_SESSION_TIME_RANGE seconds.
      val lastSessionTime = liveTelemetryMonitor.telemetryData.lastOrNull()?.sessionTime ?: 0.0
      (lastSessionTime - DEFAULT_SESSION_TIME_RANGE.toSeconds()).coerceAtLeast(0.0)
    } else {
      request.minSessionTime
    }
    val endTime = if (request.maxSessionTime > 0) request.maxSessionTime else Double.MAX_VALUE

    wrappedObserver.onNext(
      dataRange {
        min = liveTelemetryMonitor.telemetryData.firstOrNull()?.driverDistance?.toDouble() ?: 0.0
        max = liveTelemetryMonitor.telemetryData.lastOrNull()?.driverDistance?.toDouble() ?: 0.0
      }
    )

    telemetryDataStreamObserverHolders.add(
      TelemetryDataStreamObserverProgressHolder(
        responseObserver = wrappedObserver,
        sampleRateHz = hz,
        startTime = startTime,
        endTime = endTime,
      )
    )
  }

  private class QueryTelemetryResponseStreamObserver(
    private val delegate: StreamObserver<QueryTelemetryResponse>,
  ) : StreamObserver<QueryTelemetryResponse> {
    fun onNext(value: TelemetryData) = onNext(queryTelemetryResponse { data = value })

    fun onNext(value: DataRange) = onNext(queryTelemetryResponse { dataRange = value })

    override fun onNext(value: QueryTelemetryResponse) = delegate.onNext(value)

    override fun onError(t: Throwable?) = delegate.onError(t)

    override fun onCompleted() = delegate.onCompleted()
  }

  private class TelemetryDataStreamObserverProgressHolder(
    private val responseObserver: QueryTelemetryResponseStreamObserver,
    sampleRateHz: Double,
    startTime: Double,
    private val endTime: Double,
  ) {
    val entriesSent = AtomicInteger(0)
    private val delta = 1.0 / sampleRateHz
    private var lastTime = startTime

    /** Send the telemetry data and return whether we are done sending data. */
    fun sendIfNecessary(telemetryData: TelemetryData): Boolean {
      if (telemetryData.sessionTime > lastTime + delta) {
        responseObserver.onNext(telemetryData)
        lastTime += delta
      }
      entriesSent.getAndIncrement()
      val shouldClose = lastTime > endTime
      if (shouldClose) responseObserver.onCompleted()
      return shouldClose
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private val DEFAULT_SESSION_TIME_RANGE = Duration.ofMinutes(2)
  }
}
