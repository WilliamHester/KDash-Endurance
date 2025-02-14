package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.RateLimiter
import com.google.protobuf.ByteString
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.DescriptorProtos
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.api.IRacingDataReader
import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.api.VarBuffer
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.DriverHeaderOrVarBufferProto
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceGrpc
import me.williamhester.kdash.enduranceweb.proto.VarBufferFields
import me.williamhester.kdash.enduranceweb.proto.driverHeader
import me.williamhester.kdash.enduranceweb.proto.driverHeaderOrVarBufferProto
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class Client {

  private val latch = CountDownLatch(1)
  private val rateLimiter = RateLimiter.create(60.0)
  private lateinit var outputStreamObserver: StreamObserver<DriverHeaderOrVarBufferProto>

  fun connect() {
    val channel = NettyChannelBuilder.forTarget("localhost:8081").usePlaintext().build()
    val client = LiveTelemetryPusherServiceGrpc.newStub(channel)

    val responseObserver = object : StreamObserver<VarBufferFields> {
      override fun onNext(value: VarBufferFields) {
        onConnected(value)
      }

      override fun onError(t: Throwable) {
        logger.atWarning().withCause(t).log("Error")
      }

      override fun onCompleted() {
        logger.atInfo().log("Stream completed.")
      }
    }
    outputStreamObserver = client.connect(responseObserver)
    latch.countDown()
  }

  fun onConnected(varBufferFields: VarBufferFields) {
    latch.await()
    val iRacingDataReader = IRacingLoggedDataReader(Paths.get("/Users/williamhester/Downloads/livedata.ibt")) // IRacingLiveDataReader()

    val driverCarIdx = iRacingDataReader.metadata["DriverInfo"]["DriverCarIdx"].value
    val driverCar = iRacingDataReader.metadata["DriverInfo"]["Drivers"].first { it["CarIdx"].value == driverCarIdx }

    val driverHeader = driverHeader {
      name = driverCar["UserName"].value
    }
    outputStreamObserver.onNext(driverHeaderOrVarBufferProto { this.header = driverHeader })

    val byteArrayOutputStream = ByteArrayOutputStream()
    val codedOutputStream = CodedOutputStream.newInstance(byteArrayOutputStream)

    while (iRacingDataReader.hasNext()) {
      rateLimiter.acquire()
      byteArrayOutputStream.reset()

      val data = iRacingDataReader.next()
      for (field in varBufferFields.descriptorProto.fieldList) {
        data.writeToOutputStream(field, iRacingDataReader, codedOutputStream)
      }

      codedOutputStream.flush()

      val bytes = byteArrayOutputStream.toByteArray()
      logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log("Bytes: %s", bytes.size)
      val dataSnapshot = DataSnapshot.parseFrom(bytes)
      outputStreamObserver.onNext(
        driverHeaderOrVarBufferProto {
          this.dataSnapshot = dataSnapshot
        }
      )
      logger.atInfo().atMostEvery(10, TimeUnit.SECONDS).log("Sending data snapshot: %s", dataSnapshot)
    }
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

    @JvmStatic
    fun main(args: Array<String>) {
      Thread.setDefaultUncaughtExceptionHandler {
        _, e ->
          logger.atSevere().withCause(e).log("Uncaught exception")
      }
      Client().connect()
      Thread.sleep(365 * 24 * 60 * 60 * 1000)
    }
  }
}
