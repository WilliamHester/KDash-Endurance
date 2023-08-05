package me.williamhester.kdash.web

import io.grpc.ServerBuilder

fun main() {
  ServerBuilder.forPort(8081)
    .addService(LiveTelemetryService())
    .build()
    .start()
    .awaitTermination()
}