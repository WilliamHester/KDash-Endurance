package me.williamhester.kdash.web

import io.grpc.ServerBuilder
import java.util.concurrent.Executors

fun main() {
  val executor = Executors.newCachedThreadPool()
  val telemetryService = LiveTelemetryService()
  executor.execute(telemetryService)

  ServerBuilder.forPort(8081)
    .addService(telemetryService)
    .build()
    .start()
    .awaitTermination()
}
