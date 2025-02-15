package me.williamhester.kdash.web

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.grpc.ServerBuilder
import me.williamhester.kdash.web.state.DataSnapshotQueue
import me.williamhester.kdash.web.state.MetadataHolder
import java.util.concurrent.Executors

internal class ServerRunner {
  fun run() {
    val executor = Executors.newCachedThreadPool(
      ThreadFactoryBuilder()
        .setNameFormat("runner-thread-%d")
        .setUncaughtExceptionHandler { _, e -> e?.printStackTrace() }
        .build()
    )
    val dataSnapshotQueue = DataSnapshotQueue()
    val metadataHolder = MetadataHolder()
    val telemetryService = LiveTelemetryService(metadataHolder, dataSnapshotQueue)
    telemetryService.start(executor)
    val liveTelemetryPusherService = LiveTelemetryPusherService(metadataHolder, dataSnapshotQueue)

    ServerBuilder.forPort(8081)
      .addService(telemetryService)
      .addService(liveTelemetryPusherService)
      .executor(Executors.newCachedThreadPool(ThreadFactoryBuilder().setNameFormat("grpc-thread-%d").build()))
      .build()
      .start()
      .awaitTermination()
  }
}