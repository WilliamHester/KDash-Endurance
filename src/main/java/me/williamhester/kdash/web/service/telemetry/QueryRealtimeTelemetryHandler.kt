package me.williamhester.kdash.web.service.telemetry

import com.google.common.flogger.FluentLogger
import io.grpc.stub.StreamObserver
import me.williamhester.kdash.enduranceweb.proto.QueryRealtimeTelemetryRequest
import me.williamhester.kdash.enduranceweb.proto.QueryRealtimeTelemetryResponse
import me.williamhester.kdash.enduranceweb.proto.queryRealtimeTelemetryResponse
import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.models.TelemetryDataPoint
import me.williamhester.kdash.web.models.TelemetryRange
import me.williamhester.kdash.web.query.Query
import me.williamhester.kdash.web.store.Store
import me.williamhester.kdash.web.store.StreamedResponseListener

internal class QueryRealtimeTelemetryHandler(
  request: QueryRealtimeTelemetryRequest,
  private val responseObserver: StreamObserver<QueryRealtimeTelemetryResponse>,
) : Runnable, StreamedResponseListener<TelemetryDataPoint> {
  private val sampleRateHz = request.sampleRateHz
  private val delta = 1.0 / sampleRateHz
  private var lastTime = 0.0
  private val processors = request.queriesList.map(Query::parse)
  // Initialize a list of previous values to Double.NEGATIVE_INFINITY. This should ensure that the first values read
  // are different from the values that already existed in the list.
  private val previousValues = (1..request.queriesCount).map { Double.NEGATIVE_INFINITY }.toMutableList()
  private val sessionKey = with(request.sessionIdentifier) {
    SessionKey(sessionId, subSessionId, simSessionNumber, carNumber)
  }

  override fun run() {
    // The lap offset required to properly display the latest data.
    //
    // For example, if we are displaying a value that's the average of the last 5 laps, we need to process the previous
    // 5 laps of data before we can properly display the results for this value.
    val largestLapOffsetRequired = processors.maxOf { it.requiredOffset }
    val range = Store.getSessionTelemetryRange(sessionKey) ?: TelemetryRange(0.0, 0.0, 0.0F, 0.0F)

    val minTime =
      Store.getSessionTimeForTargetDistance(sessionKey, range.maxDriverDistance - largestLapOffsetRequired)
        ?: range.minSessionTime

    // Send all data we're aware of except the last value to the processors only
    Store.getTelemetryForRange(sessionKey, minTime, range.maxSessionTime - 0.001, sampleRateHz) {
      processors.map { p -> p.process(it) }
    }
    // Send data starting with the latest value.
    Store.getTelemetryForRange(sessionKey, range.maxSessionTime - 0.001, Double.MAX_VALUE, sampleRateHz, this)
    logger.atInfo().log("Finished QueryRealTimeTelemetry")
  }

  override fun onNext(value: TelemetryDataPoint) {
    val results = processors.map { it.process(value) }
    if (value.sessionTime > lastTime + delta) {
      val resultValues = results.map(DataPoint::value)
      val response = queryRealtimeTelemetryResponse {
        var i = 0
        for ((prev, cur) in previousValues.zip(resultValues)) {
          if (prev != cur) {
            previousValues[i] = cur
            sparseQueryValues[i] = cur
          }
          i++
        }
      }
      if (response.sparseQueryValuesCount > 0) {
        responseObserver.onNext(response)
      }
      lastTime += delta
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}
