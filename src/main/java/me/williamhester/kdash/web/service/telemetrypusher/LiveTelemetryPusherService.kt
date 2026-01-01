package me.williamhester.kdash.web.service.telemetrypusher

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceGrpc.LiveTelemetryPusherServiceImplBase
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot
import me.williamhester.kdash.enduranceweb.proto.VarBufferFieldsOrControlMessage
import me.williamhester.kdash.enduranceweb.proto.varBufferFields
import me.williamhester.kdash.enduranceweb.proto.varBufferFieldsOrControlMessage
import me.williamhester.kdash.web.common.SynchronizedStreamObserver

class LiveTelemetryPusherService : LiveTelemetryPusherServiceImplBase() {
  private val sessionConnectionRegistry = SessionConnectionRegistry()

  override fun connect(
    responseObserver: StreamObserver<VarBufferFieldsOrControlMessage>,
  ): StreamObserver<SessionMetadataOrDataSnapshot> {
    responseObserver.onNext(VAR_BUFFER_FIELDS)
    logger.atInfo().log("Sent VAR_BUFFER_FIELDS: %s", VAR_BUFFER_FIELDS)
    return SynchronizedStreamObserver(DriverConnection(sessionConnectionRegistry, responseObserver))
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private val VAR_BUFFER_FIELDS = varBufferFieldsOrControlMessage {
      varBufferFields = varBufferFields {
        descriptorProto = DataSnapshot.getDescriptor().toProto()
      }
    }
  }
}