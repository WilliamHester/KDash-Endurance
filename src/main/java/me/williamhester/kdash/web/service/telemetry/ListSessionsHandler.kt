package me.williamhester.kdash.web.service.telemetry

import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.ListSessionsResponse
import me.williamhester.kdash.enduranceweb.proto.listSessionsResponse
import me.williamhester.kdash.enduranceweb.proto.session
import me.williamhester.kdash.web.extensions.ProtoExtensions.toProtoTimestamp
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.store.Store
import java.util.Locale

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
        sessionName = formatSessionName(
          it.sessionMetadata["SessionInfo"]["Sessions"][it.simSessionNumber]["SessionName"].value
        )
        val driverCarIdx = it.sessionMetadata["DriverInfo"]["DriverCarIdx"].value.toInt()
        mostRecentDriver = it.sessionMetadata["DriverInfo"]["Drivers"].listList.firstOrNull { driver ->
          driver["CarIdx"].value.toInt() == driverCarIdx
        }?.get("UserName")?.value ?: "Unknown"
      }
    }
    streamObserver.onNext(
      listSessionsResponse {
        sessions += sessionCars
      }
    )
  }
}

private fun formatSessionName(sessionName: String): String {
  return sessionName.splitToSequence(' ').joinToString(" ") {
    it.lowercase().replaceFirstChar { firstChar ->
      if (firstChar.isLowerCase()) firstChar.titlecase(Locale.getDefault()) else firstChar.toString()
    }
  }
}