package me.williamhester.kdash.web.query

import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.TelemetryDataPoint
import me.williamhester.kdash.web.query.DecreasingSumProcessor.MonotonicRange.RangeType.DECREASING
import me.williamhester.kdash.web.query.DecreasingSumProcessor.MonotonicRange.RangeType.INCREASING

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
    runningTotal += current.value

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

    val interpolatedValue = interpolateDistance(currentDistance - laps, oldPoint1, oldPoint2)

    val result = (runningTotal + interpolatedValue) / (queue.size + 1)

    return DataPoint(current.sessionTime, current.driverDistance, result)
  }
}

internal class DecreasingSumProcessor(private val childProcessor: Processor, private val laps: Int) : Processor {

  private var monotonicRanges = ArrayDeque<MonotonicRange>(5)

  override val requiredOffset: Float = laps + childProcessor.requiredOffset

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
    val current = childProcessor.process(telemetryDataPoint)
    val lastRange = monotonicRanges.lastOrNull()
    if (lastRange == null) {
      monotonicRanges.add(MonotonicRange(current))
    } else {
      val currentValue = current.value
      val lastRangeValue = lastRange.values.last().value
      when (lastRange.rangeType) {
        null -> {
          if (currentValue > lastRangeValue) {
            lastRange.rangeType = INCREASING
          } else if (currentValue < lastRangeValue) {
            lastRange.rangeType = DECREASING
          }
          lastRange.values.add(current)
        }
        DECREASING -> {
          if (currentValue <= lastRangeValue) {
            lastRange.values.add(current)
          } else {
            monotonicRanges.add(MonotonicRange(current))
          }
        }
        INCREASING -> {
          if (currentValue >= lastRangeValue) {
            lastRange.values.add(current)
          } else {
            monotonicRanges.add(MonotonicRange(current))
          }
        }
      }
    }

    val currentDistance = telemetryDataPoint.driverDistance
    var endOfRange: DataPoint? = null
    outer@ while (monotonicRanges.size > 0) {
      val firstRange = monotonicRanges.first()
      var i = 0
      while (i < firstRange.values.size) {
        val last = firstRange.values[i]
        if (last.driverDistance > currentDistance - laps) {
          // At this point, i is 1 more than the endOfRange data position
          for (unused in 0 until i - 1) {
            firstRange.removeFirst()
          }
          break@outer
        }
        endOfRange = last
        i++
      }
      monotonicRanges.removeFirst()
    }

    // Return 0 if we don't have a data point
    if (endOfRange == null) return DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, 0.0)

    val oldPoint1 = endOfRange
    val oldPoint2 = when {
      monotonicRanges.first().values.size > 1 -> monotonicRanges.first().values[1]
      monotonicRanges.size > 1 -> monotonicRanges[1].values.first()
      else -> endOfRange // Fall back in case there's no other value.
    }

    val interpolatedValue = interpolateDistance(currentDistance - laps, oldPoint1, oldPoint2)
    val firstRange = monotonicRanges.first()
    val firstRangeValue = if (firstRange.rangeType == DECREASING) {
      interpolatedValue - firstRange.values.last().value
    } else {
      0.0
    }

    var result = 0.0
    for (range in monotonicRanges.iterator().apply { next() }) {
      if (range.rangeType == DECREASING) {
        result += range.delta
      }
    }
    result += firstRangeValue

    return DataPoint(current.sessionTime, current.driverDistance, result)
  }

  private class MonotonicRange(firstValue: DataPoint) {
    val values = ArrayDeque<DataPoint>(36_000).apply { add(firstValue) }

    /** Whether it's determined that the range is increasing or decreasing */
    var rangeType: RangeType? = null

    val delta: Double
      get() = values.first().value - values.last().value

    fun removeFirst() {
      values.removeFirst()
    }

    enum class RangeType {
      INCREASING,
      DECREASING,
    }
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
