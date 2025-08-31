package me.williamhester.kdash.web

import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.grpc.ServerBuilder
import me.williamhester.kdash.web.service.telemetry.LiveTelemetryService
import me.williamhester.kdash.web.service.telemetrypusher.LiveTelemetryPusherService
import java.util.concurrent.Executors

internal class ServerRunner {
  fun run() {
    val executor = MoreExecutors.listeningDecorator(
      Executors.newCachedThreadPool(
        ThreadFactoryBuilder()
          .setNameFormat("LiveTelemetryService-%d")
          .setUncaughtExceptionHandler { _, e -> e?.printStackTrace() }
          .build()
      )
    )
    val telemetryService = LiveTelemetryService(executor)
    val liveTelemetryPusherService = LiveTelemetryPusherService()

    ServerBuilder.forPort(8081)
      .addService(telemetryService)
      .addService(liveTelemetryPusherService)
      .executor(Executors.newCachedThreadPool(ThreadFactoryBuilder().setNameFormat("grpc-thread-%d").build()))
      .build()
      .start()
      .awaitTermination()
  }
}