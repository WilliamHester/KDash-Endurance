package me.williamhester.kdash.web

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.grpc.ServerBuilder
import me.williamhester.kdash.api.IRacingDataReader
import java.util.concurrent.Executors

internal class ServerRunner(
  private val iRacingDataReader: IRacingDataReader,
) {
  fun run() {
    val executor = Executors.newCachedThreadPool(
      ThreadFactoryBuilder()
        .setNameFormat("runner-thread-%d")
        .setUncaughtExceptionHandler { _, e -> e?.printStackTrace() }
        .build()
    )
    val telemetryService = LiveTelemetryService(iRacingDataReader)
    telemetryService.start(executor)

    ServerBuilder.forPort(8081)
      .addService(telemetryService)
      .executor(Executors.newCachedThreadPool(ThreadFactoryBuilder().setNameFormat("grpc-thread-%d").build()))
      .build()
      .start()
      .awaitTermination()
  }
}