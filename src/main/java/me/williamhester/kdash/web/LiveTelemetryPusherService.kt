package me.williamhester.kdash.web

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceGrpc.LiveTelemetryPusherServiceImplBase
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot
import me.williamhester.kdash.enduranceweb.proto.VarBufferFields
import me.williamhester.kdash.enduranceweb.proto.varBufferFields
import me.williamhester.kdash.web.state.DataSnapshotQueue
import me.williamhester.kdash.web.state.MetadataHolder
import java.util.concurrent.TimeUnit

class LiveTelemetryPusherService(
  private val metadataHolder: MetadataHolder,
  private val dataSnapshotQueue: DataSnapshotQueue,
) : LiveTelemetryPusherServiceImplBase() {
  override fun connect(
    responseObserver: StreamObserver<VarBufferFields>,
  ): StreamObserver<SessionMetadataOrDataSnapshot> {
    responseObserver.onNext(VAR_BUFFER_FIELDS)
    logger.atInfo().log("Sent VAR_BUFFER_FIELDS: %s", VAR_BUFFER_FIELDS)
    return ConnectedDriverStreamHandler()
  }

  inner class ConnectedDriverStreamHandler : StreamObserver<SessionMetadataOrDataSnapshot> {
    override fun onNext(sessionMetadataOrDataSnapshot: SessionMetadataOrDataSnapshot) {
      logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log("Received %s", sessionMetadataOrDataSnapshot)
      when {
        sessionMetadataOrDataSnapshot.hasDataSnapshot() -> {
          dataSnapshotQueue.add(sessionMetadataOrDataSnapshot.dataSnapshot)
        }
        sessionMetadataOrDataSnapshot.hasSessionMetadata() -> {
          metadataHolder.metadata = sessionMetadataOrDataSnapshot.sessionMetadata
        }
      }
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