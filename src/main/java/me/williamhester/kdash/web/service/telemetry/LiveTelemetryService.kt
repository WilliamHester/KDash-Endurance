package me.williamhester.kdash.web.service.telemetry

import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.ListeningExecutorService
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

class LiveTelemetryService(
  private val executor: ListeningExecutorService,
) : LiveTelemetryServiceImplBase() {

  override fun queryTelemetry(
    request: QueryTelemetryRequest,
    responseObserver: StreamObserver<QueryTelemetryResponse>
  ) {
    executor.submit(Wrapper(responseObserver, QueryTelemetryHandler(request, responseObserver)))
  }

  override fun queryRealtimeTelemetry(
    request: QueryRealtimeTelemetryRequest,
    responseObserver: StreamObserver<QueryRealtimeTelemetryResponse>
  ) {
    executor.submit(Wrapper(responseObserver, QueryRealtimeTelemetryHandler(request, responseObserver)))
  }

  override fun monitorLaps(request: ConnectRequest, responseObserver: StreamObserver<LapData>) {
    executor.submit(Wrapper(responseObserver, MonitorLapsHandler(responseObserver, executor)))
  }

  override fun monitorCurrentDrivers(request: ConnectRequest, responseObserver: StreamObserver<CurrentDrivers>) {
    executor.submit(Wrapper(responseObserver, MonitorCurrentDriversHandler(responseObserver)))
  }

  override fun monitorCurrentGaps(request: ConnectRequest, responseObserver: StreamObserver<Gaps>) {
    super.monitorCurrentGaps(request, responseObserver)
  }

  override fun monitorDriverDistances(request: ConnectRequest, responseObserver: StreamObserver<DriverDistances>) {
    super.monitorDriverDistances(request, responseObserver)
  }

  private class Wrapper(private val streamObserver: StreamObserver<*>, private val delegate: Runnable) : Runnable {
    override fun run() {
      try {
        delegate.run()
        streamObserver.onCompleted()
        logger.atInfo().log("Completed.")
      } catch (t: Throwable) {
        logger.atWarning().withCause(t).log("Error while writing response.")
        streamObserver.onError(t)
      }
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}