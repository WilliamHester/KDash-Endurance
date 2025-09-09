package me.williamhester.kdash.web.service.telemetry

import com.google.common.base.Stopwatch
import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.ListeningExecutorService
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ConnectRequest
import me.williamhester.kdash.enduranceweb.proto.CurrentDrivers
import me.williamhester.kdash.enduranceweb.proto.DriverDistances
import me.williamhester.kdash.enduranceweb.proto.Gaps
import me.williamhester.kdash.enduranceweb.proto.LapData
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryServiceGrpc.LiveTelemetryServiceImplBase
import me.williamhester.kdash.enduranceweb.proto.QueryRealtimeTelemetryRequest
import me.williamhester.kdash.enduranceweb.proto.QueryRealtimeTelemetryResponse
import me.williamhester.kdash.enduranceweb.proto.QueryTelemetryRequest
import me.williamhester.kdash.enduranceweb.proto.QueryTelemetryResponse
import java.time.Duration
import java.util.concurrent.TimeUnit.MILLISECONDS

class LiveTelemetryService(
  private val executor: ListeningExecutorService,
) : LiveTelemetryServiceImplBase() {

  override fun queryTelemetry(
    request: QueryTelemetryRequest,
    responseObserver: StreamObserver<QueryTelemetryResponse>
  ) {
    executeHandler("QueryTelemetry", responseObserver, QueryTelemetryHandler(request, responseObserver))
  }

  override fun queryRealtimeTelemetry(
    request: QueryRealtimeTelemetryRequest,
    responseObserver: StreamObserver<QueryRealtimeTelemetryResponse>
  ) {
    executeHandler("QueryRealtimeTelemetry", responseObserver, QueryRealtimeTelemetryHandler(request, responseObserver))
  }

  override fun monitorLaps(request: ConnectRequest, responseObserver: StreamObserver<LapData>) {
    executeHandler("MonitorLaps", responseObserver, MonitorLapsHandler(responseObserver, executor))
  }

  override fun monitorCurrentDrivers(request: ConnectRequest, responseObserver: StreamObserver<CurrentDrivers>) {
    executeHandler("MonitorCurrentDrivers", responseObserver, MonitorCurrentDriversHandler(responseObserver))
  }

  override fun monitorCurrentGaps(request: ConnectRequest, responseObserver: StreamObserver<Gaps>) {
    super.monitorCurrentGaps(request, responseObserver)
  }

  override fun monitorDriverDistances(request: ConnectRequest, responseObserver: StreamObserver<DriverDistances>) {
    super.monitorDriverDistances(request, responseObserver)
  }

  private fun executeHandler(rpcName: String, streamObserver: StreamObserver<*>, delegate: Runnable) {
    val future = executor.submit {
      val stopwatch = Stopwatch.createStarted()
      try {
        logger.atInfo().log("Handling LiveTelemetryService.%s on thread %s", rpcName, Thread.currentThread().name)
        delegate.run()
        val durationMs = stopwatch.elapsed(MILLISECONDS)
        val duration = Duration.ofMillis(durationMs)
        logger.atInfo().log("Completed LiveTelemetryService.%s\nDuration: %s", rpcName, duration)
        streamObserver.onCompleted()
      } catch (e: InterruptedException) {
        logger.atInfo().log(
          "Cancelled LiveTelemetryService.%s\nDuration: %s", rpcName, stopwatch.elapsed(MILLISECONDS).toDuration())
        streamObserver.onError(e)
      } catch (t: Throwable) {
        logger.atWarning().withCause(t).log("Error while writing response for LiveTelemetryService.%s", rpcName)
        streamObserver.onError(t)
      }
    }
    val serverCallStreamObserver = streamObserver as ServerCallStreamObserver
    serverCallStreamObserver.setOnCancelHandler {
      future.cancel(true)
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}

private fun Long.toDuration(): Duration = Duration.ofMillis(this)
