package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import kotlin.io.path.Path

internal object LocalClient {

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

  @JvmStatic
  fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler {
        _, e ->
      logger.atSevere().withCause(e).log("Uncaught exception")
    }
//    val loggedDataReader = IRacingLoggedDataReader(Paths.get("/Users/williamhester/Downloads/livedata.ibt"))
//    val basePath = Path("/Users/williamhester/Documents/iRacing/telemetry/multiclass")
    val basePath = Path("/Users/williamhester/Documents/iRacing/telemetry/IMSA_watkins_practice")
    val loggedDataReader = IRacingLoggedDataAndMetadataReader(
      ibtFilePath = basePath.resolve("telemetry.ibt"),
      sessionInfoFilePath = basePath.resolve("sessioninfo.irh"),
    )
    Client(loggedDataReader, channel).start()
    Thread.sleep(365 * 24 * 60 * 60 * 1000L)
  }
}
