package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.RateLimiter
import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.DescriptorProtos
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.api.IRacingDataReader
import me.williamhester.kdash.api.VarBuffer
import me.williamhester.kdash.enduranceweb.proto.ControlMessage
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceGrpc
import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.enduranceweb.proto.SessionMetadataOrDataSnapshot
import me.williamhester.kdash.enduranceweb.proto.VarBufferFields
import me.williamhester.kdash.enduranceweb.proto.VarBufferFieldsOrControlMessage
import me.williamhester.kdash.enduranceweb.proto.sessionMetadata
import me.williamhester.kdash.enduranceweb.proto.sessionMetadataOrDataSnapshot
import me.williamhester.kdash.web.common.SynchronizedStreamObserver
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


internal class Client(
  private val iRacingDataReader: IRacingDataReader,
  private val channel: ManagedChannel,
) {
  private val latch = CountDownLatch(1)
  private val rateLimiter = RateLimiter.create(600.0)
  private lateinit var outputStreamObserver: StreamObserver<SessionMetadataOrDataSnapshot>
  private val sessionMetadataMonitorExecutor = Executors.newSingleThreadScheduledExecutor()
  private var lastSessionMetadataVersion: Int = -1
  private var sessionMetadataMonitor: ScheduledFuture<*>? = null
  private var shouldSend = false

  fun connect() {
    val client = LiveTelemetryPusherServiceGrpc.newStub(channel)

    val responseObserver = object : StreamObserver<VarBufferFieldsOrControlMessage> {
      override fun onNext(value: VarBufferFieldsOrControlMessage) {
        when {
          value.hasVarBufferFields() -> onConnected(value.varBufferFields)
          value.hasControlMessage() -> handleControlMessage(value.controlMessage)
        }
      }

      override fun onError(t: Throwable) {
        logger.atWarning().withCause(t).log("Error")
        sessionMetadataMonitor?.cancel(false)
      }

      override fun onCompleted() {
        logger.atInfo().log("Stream completed.")
        sessionMetadataMonitor?.cancel(false)
      }
    }
    outputStreamObserver = SynchronizedStreamObserver(client.connect(responseObserver))
    latch.countDown()
  }

  fun handleControlMessage(controlMessage: ControlMessage) {
    val command = controlMessage.command
    when (command) {
      ControlMessage.ControlCommand.STOP_SENDING -> shouldSend = false
      ControlMessage.ControlCommand.START_SENDING -> shouldSend = true
      else -> logger.atWarning().log("Unknown command $command")
    }
  }

  fun onConnected(varBufferFields: VarBufferFields) {
    latch.await()

    sendSessionMetadata()
    sessionMetadataMonitor =
      sessionMetadataMonitorExecutor.scheduleAtFixedRate(this::sendSessionMetadata, 1, 1, TimeUnit.SECONDS)

    val byteArrayOutputStream = ByteArrayOutputStream()
    val codedOutputStream = CodedOutputStream.newInstance(byteArrayOutputStream)

    while (iRacingDataReader.hasNext()) {
      rateLimiter.acquire()
      byteArrayOutputStream.reset()

      val data = iRacingDataReader.next()
      checkDriverInCar(data)

      if (!shouldSend) continue

      for (field in varBufferFields.descriptorProto.fieldList) {
        data.writeToOutputStream(field, iRacingDataReader, codedOutputStream)
      }

      codedOutputStream.flush()

      val bytes = byteArrayOutputStream.toByteArray()
      val dataSnapshot = DataSnapshot.parseFrom(bytes)
      outputStreamObserver.onNext(
        sessionMetadataOrDataSnapshot {
          this.dataSnapshot = dataSnapshot
        }
      )
    }
  }

  private fun checkDriverInCar(varBuffer: VarBuffer) {
    val isThisClientInCar = varBuffer.getBoolean("IsOnTrack")
    val wasSending = shouldSend
    shouldSend = isThisClientInCar || shouldSend
    if (!wasSending && isThisClientInCar) {
      logger.atInfo().log("Client in car. Sending data.")
    }
  }

  private fun sendSessionMetadata() {
    val sessionInfoVersion = iRacingDataReader.fileHeader.sessionInfoUpdate
    if (lastSessionMetadataVersion == sessionInfoVersion) return

    logger.atInfo().log(
      "New session metadata available. New version: %s; Old version: %s",
      sessionInfoVersion,
      lastSessionMetadataVersion,
    )

    lastSessionMetadataVersion = sessionInfoVersion

    outputStreamObserver.onNext(
      sessionMetadataOrDataSnapshot {
        this.sessionMetadata = iRacingDataReader.metadata.toProto()
      }
    )
  }

  private fun VarBuffer.writeToOutputStream(
    field: DescriptorProtos.FieldDescriptorProto,
    iRacingDataReader: IRacingDataReader,
    codedOutputStream: CodedOutputStream,
  ) {
    val iracingField = field.options.unknownFields.getField(50000)
    val iRacingFieldName = ByteString.copyFrom(iracingField.lengthDelimitedList).toStringUtf8()
    val fieldNumber = field.number
    val isRepeated = field.label == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED
    val fieldHeader =  iRacingDataReader.headers[iRacingFieldName]
    if (fieldHeader == null) {
      logger.atWarning().log("Field header not found for variable '%s'. Skipping.", iRacingFieldName)
      return
    }
    val count = if (isRepeated) fieldHeader.count else 1
    for (i in 0 until count) {
      when (field.type) {
        DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE -> {
          val result = getArrayDouble(iRacingFieldName, i)
          codedOutputStream.writeDouble(fieldNumber, result)
        }
        DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT -> {
          val result = getArrayFloat(iRacingFieldName, i)
          codedOutputStream.writeFloat(fieldNumber, result)
        }
        DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32 -> {
          val result = getArrayInt(iRacingFieldName, i)
          codedOutputStream.writeInt32(fieldNumber, result)
        }
        DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL -> {
          val result = getArrayBoolean(iRacingFieldName, i)
          codedOutputStream.writeBool(fieldNumber, result)
        }
        else -> TODO("Unsupported field type: ${field.type}")
      }
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}

private fun IRacingDataReader.SessionMetadata.toProto(): SessionMetadata {
  return sessionMetadata {
    value = this@toProto.value
    keyValuePairs.putAll(this@toProto.map.entries.associate { it.key to it.value.toProto() })
    list.addAll(this@toProto.list.map { it.toProto() })
  }
}
