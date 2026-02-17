package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import me.williamhester.kdash.api.IRacingDataReader
import kotlin.io.path.Path

internal object LocalMultiDriverClient {

  private val logger = FluentLogger.forEnclosingClass()

  private val channel = NettyChannelBuilder.forAddress("127.0.0.1", 8000)
    .sslContext(
      GrpcSslContexts.forClient()
        .trustManager(
          Path("/Users/williamhester/Library/Application Support/Caddy/certificates/local/localhost/localhost.crt")
            .toFile()
        )
        .build()
    )
//    .usePlaintext()
    .build()

  private fun synchronizeReaders(reader1: IRacingDataReader, reader2: IRacingDataReader) {
    val first1 = reader1.next()
    val first2 = reader2.next()

    val first1SessionTime = first1.getDouble("SessionTime")
    val first2SessionTime = first2.getDouble("SessionTime")

    fun IRacingDataReader.discardTo(time: Double) {
      while (hasNext()) {
        val next = next()
        if (next.getDouble("SessionTime") >= time) break
      }
    }

    when {
      first1SessionTime < first2SessionTime -> reader1.discardTo(first2SessionTime)
      first2SessionTime < first1SessionTime -> reader2.discardTo(first1SessionTime)
    }
  }

  @JvmStatic
  fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler {
        _, e ->
      logger.atSevere().withCause(e).log("Uncaught exception")
    }
    val basePath = Path("/Users/williamhester/Documents/iRacing/telemetry/ratl-multidriver")
    val driver1DataReader = IRacingLoggedDataAndMetadataReader(
      ibtFilePath = basePath.resolve("driver1/telemetry.ibt"),
      sessionInfoFilePath = basePath.resolve("driver1/sessioninfo.irh"),
    )
    val driver2DataReader = IRacingLoggedDataAndMetadataReader(
      ibtFilePath = basePath.resolve("driver2/telemetry.ibt"),
      sessionInfoFilePath = basePath.resolve("driver2/sessioninfo.irh"),
    )
    synchronizeReaders(driver1DataReader, driver2DataReader)
    Client(driver1DataReader, channel, "William").start()
    Client(driver2DataReader, channel, "Matthew").start()
    Thread.sleep(365 * 24 * 60 * 60 * 1000L)
  }
}
