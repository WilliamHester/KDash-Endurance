package me.williamhester.kdash.web

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ControlMessage.ControlCommand
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceGrpc.LiveTelemetryPusherServiceImplBase
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot.ValueCase.DATA_SNAPSHOT
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot.ValueCase.SESSION_METADATA
import me.williamhester.kdash.enduranceweb.proto.VarBufferFieldsOrControlMessage
import me.williamhester.kdash.enduranceweb.proto.controlMessage
import me.williamhester.kdash.enduranceweb.proto.varBufferFields
import me.williamhester.kdash.enduranceweb.proto.varBufferFieldsOrControlMessage
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.models.SessionInfo
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
    private lateinit var liveTelemetryDataWriter: LiveTelemetryDataWriter

    override fun onNext(sessionMetadataOrDataSnapshot: SessionMetadataOrDataSnapshot) {
      when (sessionMetadataOrDataSnapshot.valueCase) {
        DATA_SNAPSHOT -> {
          val isOnTrack = sessionMetadataOrDataSnapshot.dataSnapshot.isOnTrack
          if (isOnTrack && !wasOnTrack) {
            // Tell any other clients to stop publishing data, since the driver at this client is actually in the car.
            handleClientOnTrack(this)
            isSendingData = true
          }
          wasOnTrack = isOnTrack
          dataSnapshotQueue.add(sessionMetadataOrDataSnapshot.dataSnapshot)
          liveTelemetryDataWriter.onDataSnapshot(sessionMetadataOrDataSnapshot.dataSnapshot)
        }
        SESSION_METADATA -> {
          val metadata = sessionMetadataOrDataSnapshot.sessionMetadata
          // Note: This makes a new LiveTelemetryDataWriter every time there's a new metadata string. This is really
          // just out of convenience, because the session number may have changed (e.g. practice -> qualifying), and
          // the LiveTelemetryDataWriter is otherwise stateless.
          liveTelemetryDataWriter = LiveTelemetryDataWriter(metadata.sessionInfo())
          metadataHolder.metadata = metadata
        }
        else -> {
          logger.atWarning().log("Unknown value: %s", sessionMetadataOrDataSnapshot)
        }
      }
    }

    private fun SessionMetadata.sessionInfo(): SessionInfo {
      val sessionId = this["WeekendInfo"]["SessionID"].value.toInt()
      val subSessionId = this["WeekendInfo"]["SubSessionID"].value.toInt()
      val simSessionNumber = this["SessionInfo"]["SimSessionNumber"].value.ifBlank { "0" }.toInt()
      val driverCarIdx = this["DriverInfo"]["DriverCarIdx"].value.toInt()
      // DriverInfo:Drivers:idx:CarNumber and ...:CarNumberRaw both exist. However, CarNumberRaw is sometimes over 1000
      // and it's not clear why. Removing the quotes from CarNumber seems to more accurately get what's displayed.
      val carNumber =
        this["DriverInfo"]["Drivers"][driverCarIdx]["CarNumber"]
          .value
          .substringAfter('"')
          .substringBefore('"')
      return SessionInfo(
        sessionId = sessionId,
        subSessionId = subSessionId,
        sessionNum = simSessionNumber,
        carNumber = carNumber,
      )
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