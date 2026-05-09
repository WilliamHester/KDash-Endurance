package me.williamhester.kdash.web.service.telemetry

import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ListSessionsResponse
import me.williamhester.kdash.enduranceweb.proto.listSessionsResponse
import me.williamhester.kdash.enduranceweb.proto.session
import me.williamhester.kdash.web.extensions.ProtoExtensions.toProtoTimestamp
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.store.Store

internal class ListSessionsHandler(
  private val streamObserver: StreamObserver<ListSessionsResponse>,
) : Runnable {
  override fun run() {
    val sessionCars = Store.listSessionCars().map {
      session {
        sessionId = it.sessionId
        subSessionId = it.subSessionId
        simSessionNumber = it.simSessionNumber
        carNumber = it.carNumber
        trackName = it.sessionMetadata["WeekendInfo"]["TrackDisplayName"].value
        sessionCreated = it.sessionCreated.toProtoTimestamp()
      }
    }
    streamObserver.onNext(
      listSessionsResponse {
        sessions += sessionCars
      }
    )
  }
}
