package me.williamhester.kdash.web.service.telemetry

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.QueryTelemetryRequest
import me.williamhester.kdash.enduranceweb.proto.QueryTelemetryResponse
import me.williamhester.kdash.enduranceweb.proto.dataRange
import me.williamhester.kdash.enduranceweb.proto.dataRanges
import me.williamhester.kdash.enduranceweb.proto.queryTelemetryResponse
import me.williamhester.kdash.enduranceweb.proto.telemetryData
import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.models.TelemetryDataPoint
import me.williamhester.kdash.web.models.TelemetryRange
import me.williamhester.kdash.web.query.Query
import me.williamhester.kdash.web.store.Store
import me.williamhester.kdash.web.store.StreamedResponseListener
import java.time.Duration

internal class QueryTelemetryHandler(
  private val request: QueryTelemetryRequest,
  private val responseObserver: StreamObserver<QueryTelemetryResponse>,
): Runnable {
  private val sessionKey = with(request.sessionIdentifier) {
    SessionKey(sessionId, subSessionId, simSessionNumber, carNumber)
  }
  private val processors = request.queriesList.map(Query::parse)

  override fun run() {
    val range = Store.getSessionTelemetryRange(sessionKey)
    sendSessionDataRanges(range)
    val (hz, startTime, endTime) = getQueryParameters(range)

    Store.getTelemetryForRange(sessionKey, startTime, endTime, hz, TelemetryDataPointReceiver(hz))
  }

  private fun sendSessionDataRanges(range: TelemetryRange?) {
    responseObserver.onNext(
      queryTelemetryResponse {
        dataRanges = dataRanges {
          sessionTime = dataRange {
            min = range?.minSessionTime ?: 0.0
            max = range?.maxSessionTime ?: 0.0
          }
          driverDistance = dataRange {
            min = range?.minDriverDistance?.toDouble() ?: 0.0
            max = range?.maxDriverDistance?.toDouble() ?: 0.0
          }
        }
      }
    )
  }

  private inner class TelemetryDataPointReceiver(hz: Double) : StreamedResponseListener<TelemetryDataPoint> {
    private val delta = 1.0 / hz
    private var lastTime = 0.0

    override fun onNext(value: TelemetryDataPoint) {
      val results = processors.map { it.process(value) }
      if (value.sessionTime > lastTime + delta) {
        val firstResult = results.first()
        responseObserver.onNext(
          queryTelemetryResponse {
            data = telemetryData {
              sessionTime = firstResult.sessionTime
              driverDistance = firstResult.driverDistance
              queryValues.addAll(results.map(DataPoint::value))
            }
          }
        )
        lastTime += delta
      }
    }
  }

  private fun getQueryParameters(range: TelemetryRange?): QueryParameters {
    val hz = if (request.sampleRateHz > 0) request.sampleRateHz else 100.0
    val startTime = if (request.minSessionTime == -1.0) {
      // -1.0 means that we should just retrieve the last DEFAULT_SESSION_TIME_RANGE seconds.
      val lastSessionTime = range?.maxSessionTime ?: 0.0
      (lastSessionTime - DEFAULT_SESSION_TIME_RANGE.toSeconds()).coerceAtLeast(0.0)
    } else {
      request.minSessionTime
    }
    val endTime = if (request.maxSessionTime > 0) request.maxSessionTime else Double.MAX_VALUE
    return QueryParameters(hz, startTime, endTime)
  }

  private data class QueryParameters(val hz: Double, val startTime: Double, val endTime: Double)

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private val DEFAULT_SESSION_TIME_RANGE = Duration.ofMinutes(2)
  }
}
