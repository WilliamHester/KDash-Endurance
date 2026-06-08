package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.RateLimiter
import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.DescriptorProtos
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
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
import java.util.concurrent.BlockingDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


internal class Client(
  private val iRacingDataReader: IRacingDataReader,
  channel: ManagedChannel,
  clientName: String? = null,
) {
  private val logTag = if (clientName != null) "Client $clientName: " else ""
  private val dataReadRateLimiter = RateLimiter.create(60.0 * 3)
  private val reconnectionRateLimiter = RateLimiter.create(1 / 10.0)
  private var outputStreamObserver: StreamObserver<SessionMetadataOrDataSnapshot>? by BlockingDelegate()
  private var varBufferFields: VarBufferFields by BlockingDelegate()
  private var sessionMetadata: SessionMetadata by BlockingDelegate()
  private val dataQueue = LinkedBlockingDeque<SessionMetadataOrDataSnapshot>(QUEUE_CAPACITY)
  private val sessionMetadataMonitorExecutor = Executors.newSingleThreadScheduledExecutor()
  private val dataReaderExecutor = Executors.newCachedThreadPool()
  private var sessionMetadataMonitor: ScheduledFuture<*>? = null
  private var shouldSend = false
  private var wasInCar = false
  private val client = LiveTelemetryPusherServiceGrpc.newStub(channel)

  private val responseObserver = object : StreamObserver<VarBufferFieldsOrControlMessage> {
    override fun onNext(value: VarBufferFieldsOrControlMessage) {
      when {
        value.hasVarBufferFields() -> onConnected(value.varBufferFields)
        value.hasControlMessage() -> handleControlMessage(value.controlMessage)
      }
    }

    override fun onError(t: Throwable) {
      logger.atWarning().withCause(t).log("Error. Reconnecting...")
      // Nulling this out will block the sender until it's ready again. Otherwise, we spam the old stream observer with
      // requests it will never be able to send.
      outputStreamObserver = null
      connect()
    }

    override fun onCompleted() {
      logger.atInfo().log("Stream completed. Exiting.")
    }
  }

  fun start() {
    dataReaderExecutor.execute { readData(varBufferFields) }
    dataReaderExecutor.execute { sendData() }
    connect()
  }

  private fun connect() {
    reconnectionRateLimiter.acquire()
    val newOutputStreamObserver = SynchronizedStreamObserver(client.connect(responseObserver))
    newOutputStreamObserver.onNext(
      sessionMetadataOrDataSnapshot {
        sessionMetadata = this@Client.sessionMetadata
      }
    )
    outputStreamObserver = newOutputStreamObserver
  }

  private fun sendData() {
    while (!Thread.interrupted()) {
      val next = dataQueue.take()
      try {
        outputStreamObserver!!.onNext(next)
      } catch (e: StatusRuntimeException) {
        // There's a chance that we try to send the data, but more data was added to the queue at the same time, so
        // adding it back would cause an error. addFirstLossy logs if we were unable to add it to the queue.
        dataQueue.addFirstLossy(next)
        outputStreamObserver!!.onError(e)
      }
    }
  }

  private fun handleControlMessage(controlMessage: ControlMessage) {
    when (val command = controlMessage.command) {
      ControlMessage.ControlCommand.STOP_SENDING -> {
        logger.atInfo().log("%sStopping sending data.", logTag)
        shouldSend = false
      }
      ControlMessage.ControlCommand.START_SENDING -> {
        logger.atInfo().log("%sStarting sending data.", logTag)
        shouldSend = true
      }
      else -> logger.atWarning().log("Unknown command $command")
    }
  }

  fun onConnected(varBufferFields: VarBufferFields) {
    this.varBufferFields = varBufferFields
  }

  private fun readData(varBufferFields: VarBufferFields) {
    sendSessionMetadata()
    sessionMetadataMonitor =
      sessionMetadataMonitorExecutor.scheduleAtFixedRate(this::pollSessionMetadata, 1, 1, TimeUnit.SECONDS)

    val byteArrayOutputStream = ByteArrayOutputStream()
    val codedOutputStream = CodedOutputStream.newInstance(byteArrayOutputStream)

    while (!Thread.interrupted() && iRacingDataReader.hasNext()) {
      dataReadRateLimiter.acquire()
      byteArrayOutputStream.reset()

      val data = iRacingDataReader.next()
      checkShouldSend(data)

      if (!shouldSend) {
        logger.atInfo().atMostEvery(5, TimeUnit.SECONDS).log("%sSkipping sending data.", logTag)
        continue
      } else {
        logger.atInfo().atMostEvery(5, TimeUnit.SECONDS).log("%sSending data.", logTag)
      }

      for (field in varBufferFields.descriptorProto.fieldList) {
        data.writeToOutputStream(field, iRacingDataReader, codedOutputStream)
      }

      codedOutputStream.flush()

      val bytes = byteArrayOutputStream.toByteArray()
      val dataSnapshot = DataSnapshot.parseFrom(bytes)
      dataQueue.addLossy(
        sessionMetadataOrDataSnapshot {
          this.dataSnapshot = dataSnapshot
        }
      )
    }
  }

  private fun checkShouldSend(varBuffer: VarBuffer) {
    if (varBuffer.getInt("SessionState", 0) == 6) {
      // 6 == irsdk_StateCoolDown, which means that the session is over and no cars are on track. Don't send any more
      // data at this point.
      logger.atInfo().atMostEvery(60, TimeUnit.SECONDS).log("%sSession ended. Not sending data.")
      shouldSend = false
      return
    }
    val isThisClientInCar = varBuffer.getBoolean("IsOnTrack", false)
    if (wasInCar != isThisClientInCar) {
      if (isThisClientInCar) {
        logger.atInfo().log("%snow in the car", logTag)
      } else {
        logger.atWarning().log("%sno longer in the car", logTag)
      }
      wasInCar = isThisClientInCar
    }
    val wasSending = shouldSend
    when {
      isThisClientInCar && wasSending -> {} // already sending data
      isThisClientInCar && !wasSending -> {
        logger.atInfo().log("%sWas not sending data, but client now in car", logTag)
        shouldSend = true
      }
      !isThisClientInCar && wasSending -> {
        logger.atInfo().atMostEvery(5, TimeUnit.SECONDS).log("%sNo longer in the car but still sending data", logTag)
      }
      !isThisClientInCar && !wasSending -> {
        logger.atInfo().atMostEvery(5, TimeUnit.SECONDS).log("%sNot in car and not sending data", logTag)
      }
    }
  }

  private fun pollSessionMetadata() {
    if (!shouldSend) return
    if (!iRacingDataReader.hasNewMetadata()) return

    sendSessionMetadata()
  }

  private fun sendSessionMetadata() {
    val sessionMetadata = iRacingDataReader.metadata.toProto()
    dataQueue.addLossy(
      sessionMetadataOrDataSnapshot {
        this.sessionMetadata = sessionMetadata
      }
    )
    this.sessionMetadata = sessionMetadata
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

  private fun <T> BlockingDeque<T>.addLossy(element: T) {
    val didAdd = this.offer(element)
    if (!didAdd) {
      logger.atWarning().atMostEvery(30, TimeUnit.SECONDS).log("%sLost data because queue was out of space.", logTag)
      this.poll()
      this.offer(element)
    }
  }

  private fun <T> BlockingDeque<T>.addFirstLossy(element: T) {
    val didAdd = this.offerFirst(element)
    if (!didAdd) {
      logger.atWarning().atMostEvery(30, TimeUnit.SECONDS).log("%sLost data because queue was out of space.", logTag)
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    // The max capacity of the buffer. Beyond this, we'll start dropping data to avoid running clients out of RAM
    // by buffering the entire session. This is 5 minutes of data (5 * 60 seconds * 60 samples per second).
    private const val QUEUE_CAPACITY = 5 * 60 * 60
  }
}

private fun IRacingDataReader.SessionMetadata.toProto(): SessionMetadata {
  return sessionMetadata {
    value = this@toProto.value
    keyValuePairs.putAll(this@toProto.map.entries.associate { it.key to it.value.toProto() })
    list.addAll(this@toProto.list.map { it.toProto() })
  }
}

private class BlockingDelegate<T, V> : ReadWriteProperty<T, V> {
  private var latchAndValue = LatchAndValue<V>()

  override fun getValue(thisRef: T, property: KProperty<*>): V {
    val currentLatchAndValue = latchAndValue
    currentLatchAndValue.latch.await()
    return currentLatchAndValue.value!!
  }

  override fun setValue(thisRef: T, property: KProperty<*>, value: V) {
    synchronized(this) {
      if (value != null) {
        latchAndValue.value = value
        latchAndValue.latch.countDown()
      } else if (latchAndValue.latch.count == 0L) {
        latchAndValue = LatchAndValue()
      }
    }
  }

  private class LatchAndValue<V> {
    val latch = CountDownLatch(1)
    var value: V? = null
  }
}
