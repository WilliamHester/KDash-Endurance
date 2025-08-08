package me.williamhester.kdash.web.query

import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.TelemetryDataPoint
import java.util.concurrent.CopyOnWriteArrayList

sealed interface Processor {
  fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint

  /** The number of laps of data required before the current point in order to emit a value. */
  val requiredOffset: Float
}

internal class LapDeltaProcessor(private val childProcessor: Processor) : Processor {
  private val queue = CopyOnWriteArrayList<DataPoint>()

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

internal class SubtractionProcessor(private val processors: List<Processor>): Processor {
  override val requiredOffset: Float = processors.maxOf(Processor::requiredOffset)

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
    val iterator = processors.iterator()

    val first = iterator.next().process(telemetryDataPoint)
    var result = first.value
    while (iterator.hasNext()) {
      result -= iterator.next().process(telemetryDataPoint).value
    }
    return DataPoint(first.sessionTime, first.driverDistance, result)
  }
}
