package me.williamhester.kdash.web

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.grpc.ServerBuilder
import java.util.concurrent.Executors

fun main() {
  val executor = Executors.newCachedThreadPool()
  val telemetryService = LiveTelemetryService()
  executor.execute(telemetryService)

  ServerBuilder.forPort(8081)
    .addService(telemetryService)
    .executor(Executors.newCachedThreadPool(ThreadFactoryBuilder().setNameFormat("grpc-thread-%d").build()))
    .build()
    .start()
    .awaitTermination()
}
