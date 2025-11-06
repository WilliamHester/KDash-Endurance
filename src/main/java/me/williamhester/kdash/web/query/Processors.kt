package me.williamhester.kdash.web.query

import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.TelemetryDataPoint

sealed interface Processor {
  fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint

  /** The number of laps of data required before the current point in order to emit a value. */
  val requiredOffset: Float
}

internal class LapDeltaProcessor(private val childProcessor: Processor) : Processor {
  private val queue = ArrayDeque<DataPoint>(36_000)

  override val requiredOffset: Float = 1.0F + childProcessor.requiredOffset

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
    val current = childProcessor.process(telemetryDataPoint)
    queue.add(current)

    val currentDistance = telemetryDataPoint.driverDistance
    var oneLapAgoData: DataPoint? = null
    var i = 0
    while (i < queue.size) {
      val last = queue[i]
      if (last.driverDistance <= currentDistance - 1) {
        oneLapAgoData = last
      } else {
        break
      }
      i++
    }
    // At this point, i is 1 more than the oneLapAgo data position
    for (unused in 0 until i - 1) queue.removeFirst()

    // Return 0 if we don't have a data point
    if (oneLapAgoData == null) return DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, 0.0)

    val oldPoint1 = oneLapAgoData
    val oldPoint2 = queue[1]

    val interpolatedValue = interpolateDistance(currentDistance - 1, oldPoint1, oldPoint2)

    return DataPoint(current.sessionTime, current.driverDistance, interpolatedValue - current.value)
  }
}

internal class LapAverageProcessor(private val childProcessor: Processor, val laps: Int) : Processor {
  private val queue = ArrayDeque<DataPoint>(36_000)

  private var runningTotal = 0.0

  override val requiredOffset: Float = laps + childProcessor.requiredOffset

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
    val current = childProcessor.process(telemetryDataPoint)
    queue.add(current)
    runningTotal += childProcessor.process(telemetryDataPoint).value

    val currentDistance = telemetryDataPoint.driverDistance
    var endOfAverage: DataPoint? = null
    var i = 0
    while (i < queue.size) {
      val last = queue[i]
      if (last.driverDistance <= currentDistance - laps) {
        endOfAverage = last
      } else {
        break
      }
      i++
    }
    // At this point, i is 1 more than the endOfAverage data position
    for (unused in 0 until i - 1) {
      val first = queue.removeFirst()
      runningTotal -= first.value
    }

    // Return 0 if we don't have a data point
    if (endOfAverage == null) return DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, 0.0)

    val oldPoint1 = endOfAverage
    val oldPoint2 = queue[1]

    val interpolatedValue = interpolateDistance(currentDistance - 1, oldPoint1, oldPoint2)

    val result = (runningTotal + interpolatedValue) / (queue.size + 1)

    return DataPoint(current.sessionTime, current.driverDistance, result)
  }
}

private fun interpolateDistance(targetDistance: Float, dataPoint1: DataPoint, dataPoint2: DataPoint): Double {
  val distance1 = dataPoint1.driverDistance
  val distance2 = dataPoint2.driverDistance

  val delta = distance2 - distance1
  val targetDelta = distance2 - targetDistance

  val dist1Pct = targetDelta / delta
  val dist2Pct = 1.0 - dist1Pct

  return dist1Pct * dataPoint1.value + dist2Pct * dataPoint2.value
}

internal class VariableProcessor(variableName: String): Processor {
  private val getter = VariableMapping.getGetter(variableName)

  override val requiredOffset: Float = 0.0F

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
    val fieldValue = getter(telemetryDataPoint)
    return DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, fieldValue)
  }
}

internal class NumberProcessor(private val const: Int): Processor {
  override val requiredOffset: Float = 0.0F

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint =
    DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, const.toDouble())
}
