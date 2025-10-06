package me.williamhester.kdash.web.service.telemetrypusher

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ControlMessage.ControlCommand
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot.ValueCase.DATA_SNAPSHOT
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot.ValueCase.SESSION_METADATA
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot.ValueCase.VALUE_NOT_SET
import me.williamhester.kdash.enduranceweb.proto.VarBufferFieldsOrControlMessage
import me.williamhester.kdash.enduranceweb.proto.controlMessage
import me.williamhester.kdash.enduranceweb.proto.varBufferFieldsOrControlMessage
import java.util.concurrent.atomic.AtomicInteger

internal class DriverConnection(
  private val sessionConnectionRegistry: SessionConnectionRegistry,
  private val responseObserver: StreamObserver<VarBufferFieldsOrControlMessage>,
) : StreamObserver<SessionMetadataOrDataSnapshot> {
  private var wasOnTrack = false
  internal var isSendingData = false
    private set
  private lateinit var liveTelemetryDataWriter: LiveTelemetryDataWriter

  /** A unique ID for this connection. */
  val connectionId = connectionCount.getAndIncrement()

  override fun onNext(sessionMetadataOrDataSnapshot: SessionMetadataOrDataSnapshot) {
    when (sessionMetadataOrDataSnapshot.valueCase) {
      DATA_SNAPSHOT -> {
        val isOnTrack = sessionMetadataOrDataSnapshot.dataSnapshot.isOnTrack
        if (isOnTrack && !wasOnTrack) {
          // Tell any other clients to stop publishing data, since the driver at this client is actually in the car.
          sessionConnectionRegistry.driverOnTrack(this)
          isSendingData = true
        }
        wasOnTrack = isOnTrack
        liveTelemetryDataWriter.onDataSnapshot(sessionMetadataOrDataSnapshot.dataSnapshot)
      }
      SESSION_METADATA -> {
        val metadata = sessionMetadataOrDataSnapshot.sessionMetadata
        liveTelemetryDataWriter = sessionConnectionRegistry.register(this, metadata.toSessionKey())
        liveTelemetryDataWriter.onSessionMetadata(metadata)
      }
      VALUE_NOT_SET -> {} // Ignore. Probably just a ping from the client that it is setting up the connection.
      else -> {
        logger.atWarning().log("Unknown value: %s", sessionMetadataOrDataSnapshot)
      }
    }
  }

  override fun onError(t: Throwable) {
    logger.atWarning().withCause(t).log()
    sessionConnectionRegistry.unregister(this)
  }

  override fun onCompleted() {
    logger.atInfo().log("Stream completed.")
    responseObserver.onCompleted()
    sessionConnectionRegistry.unregister(this)
  }

  fun requestClientStopsSendingData() {
    logger.atInfo().log("Requesting that this client stops sending data.")
    responseObserver.onNext(
      varBufferFieldsOrControlMessage {
        controlMessage = controlMessage {
          command = ControlCommand.STOP_SENDING
        }
      }
    )
    isSendingData = false
  }

  fun requestClientStartsSendingData() {
    logger.atInfo().log("Requesting that this client starts sending data.")
    isSendingData = true
    responseObserver.onNext(
      varBufferFieldsOrControlMessage {
        controlMessage = controlMessage {
          command = ControlCommand.START_SENDING
        }
      }
    )
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private val connectionCount = AtomicInteger(0)
  }
}