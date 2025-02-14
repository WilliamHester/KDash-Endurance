package me.williamhester.kdash.web

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.DriverHeaderOrVarBufferProto
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceGrpc.LiveTelemetryPusherServiceImplBase
import me.williamhester.kdash.enduranceweb.proto.VarBufferFields
import me.williamhester.kdash.enduranceweb.proto.varBufferFields
import java.util.concurrent.TimeUnit

class LiveTelemetryPusherService : LiveTelemetryPusherServiceImplBase() {
  override fun connect(
    responseObserver: StreamObserver<VarBufferFields>,
  ): StreamObserver<DriverHeaderOrVarBufferProto> {
    responseObserver.onNext(VAR_BUFFER_FIELDS)
    logger.atInfo().log("Sent VAR_BUFFER_FIELDS: %s", VAR_BUFFER_FIELDS)
    return ConnectedDriverStreamHandler()
  }

  class ConnectedDriverStreamHandler : StreamObserver<DriverHeaderOrVarBufferProto> {
    override fun onNext(driverHeaderOrVarBufferProto: DriverHeaderOrVarBufferProto) {
      logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log("Received %s", driverHeaderOrVarBufferProto)
    }

    override fun onError(t: Throwable) {
      logger.atWarning().withCause(t).log()
    }

    override fun onCompleted() {
      logger.atInfo().log("Stream completed.")
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private val VAR_BUFFER_FIELDS = varBufferFields {
      descriptorProto = DataSnapshot.getDescriptor().toProto()
    }
  }
}