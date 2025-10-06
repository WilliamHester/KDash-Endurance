package me.williamhester.kdash.web.service.telemetry

import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ListSessionsResponse
import me.williamhester.kdash.enduranceweb.proto.listSessionsResponse
import me.williamhester.kdash.web.store.Store

internal class ListSessionsHandler(
  private val streamObserver: StreamObserver<ListSessionsResponse>,
) : Runnable {
  override fun run() {
    val sessionCars = Store.listSessionCars()
    streamObserver.onNext(
      listSessionsResponse {
        sessions += sessionCars
      }
    )
  }
}
