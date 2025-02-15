package me.williamhester.kdash.web

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ControlMessage.ControlCommand
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceGrpc.LiveTelemetryPusherServiceImplBase
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot
import me.williamhester.kdash.enduranceweb.proto.VarBufferFieldsOrControlMessage
import me.williamhester.kdash.enduranceweb.proto.controlMessage
import me.williamhester.kdash.enduranceweb.proto.varBufferFields
import me.williamhester.kdash.enduranceweb.proto.varBufferFieldsOrControlMessage
import me.williamhester.kdash.web.state.DataSnapshotQueue
import me.williamhester.kdash.web.state.MetadataHolder
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LiveTelemetryPusherService(
  private val metadataHolder: MetadataHolder,
  private val dataSnapshotQueue: DataSnapshotQueue,
) : LiveTelemetryPusherServiceImplBase() {
  private val connectedDriverLock = ReentrantLock()
  private val connectedDrivers = CopyOnWriteArrayList<ConnectedDriverStreamHandler>()

  override fun connect(
    responseObserver: StreamObserver<VarBufferFieldsOrControlMessage>,
  ): StreamObserver<SessionMetadataOrDataSnapshot> = connectedDriverLock.withLock {
    responseObserver.onNext(VAR_BUFFER_FIELDS)
    logger.atInfo().log("Sent VAR_BUFFER_FIELDS: %s", VAR_BUFFER_FIELDS)
    val connectedDriverStreamHandler = ConnectedDriverStreamHandler(responseObserver)
    connectedDrivers.add(connectedDriverStreamHandler)
    ensureOneClientIsSendingData()
    return connectedDriverStreamHandler
  }

  private fun handleClientOnTrack(connectedDriverStreamHandler: ConnectedDriverStreamHandler) =
    connectedDriverLock.withLock {
      connectedDrivers
        .filterNot { it === connectedDriverStreamHandler }
        .forEach(ConnectedDriverStreamHandler::requestClientStopsSendingData)
    }

  private fun handleClientClosed(connectedDriverStreamHandler: ConnectedDriverStreamHandler) {
    connectedDrivers.remove(connectedDriverStreamHandler)
    ensureOneClientIsSendingData()
  }

  private fun ensureOneClientIsSendingData() = connectedDriverLock.withLock {
    val activeClient = connectedDrivers.firstOrNull { it.isSendingData }
    if (activeClient != null) return

    val first = connectedDrivers.firstOrNull()
    if (first == null) {
      logger.atWarning().log("No connected drivers.")
      return
    }
    first.requestClientStartsSendingData()
  }

  inner class ConnectedDriverStreamHandler(
    private val responseObserver: StreamObserver<VarBufferFieldsOrControlMessage>,
  ) : StreamObserver<SessionMetadataOrDataSnapshot> {
    private var wasOnTrack = false
    var isSendingData = false
      private set

    override fun onNext(sessionMetadataOrDataSnapshot: SessionMetadataOrDataSnapshot) {
//      logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log("Received %s", sessionMetadataOrDataSnapshot)
      when {
        sessionMetadataOrDataSnapshot.hasDataSnapshot() -> {
          val isOnTrack = sessionMetadataOrDataSnapshot.dataSnapshot.isOnTrack
          if (isOnTrack && !wasOnTrack) {
            // Tell any other clients to stop publishing data, since the driver at this client is actually in the car.
            handleClientOnTrack(this)
            isSendingData = true
          }
          wasOnTrack = isOnTrack
          dataSnapshotQueue.add(sessionMetadataOrDataSnapshot.dataSnapshot)
        }
        sessionMetadataOrDataSnapshot.hasSessionMetadata() -> {
          metadataHolder.metadata = sessionMetadataOrDataSnapshot.sessionMetadata
        }
      }
    }

    override fun onError(t: Throwable) {
      logger.atWarning().withCause(t).log()
      handleClientClosed(this)
    }

    override fun onCompleted() {
      logger.atInfo().log("Stream completed.")
      responseObserver.onCompleted()
      handleClientClosed(this)
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