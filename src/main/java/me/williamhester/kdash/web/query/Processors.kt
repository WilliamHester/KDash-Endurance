package me.williamhester.kdash.web.query

import com.google.protobuf.Descriptors.FieldDescriptor
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceOuterClass
import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.TelemetryDataPoint
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

sealed interface Processor {
  fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint

  /** The number of laps of data required before the current point in order to emit a value. */
  val requiredOffset: Float
}

internal class LapDeltaProcessor(private val childProcessor: Processor) : Processor {
  private val queue: Queue<DataPoint> = ConcurrentLinkedQueue()

  override val requiredOffset: Float = 1.0F + childProcessor.requiredOffset

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
    val current = childProcessor.process(telemetryDataPoint)
    queue.offer(current)

    val currentDistance = telemetryDataPoint.driverDistance
    var oneLapAgoData: DataPoint? = null
    while (queue.size > 0) {
      val last = queue.peek()
      if (last.driverDistance <= currentDistance - 1) {
        oneLapAgoData = queue.poll()
      } else {
        break
      }
    }

    // Return 0 if we don't have a data point
    if (oneLapAgoData == null) return DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, 0.0)

    val oldPoint1 = oneLapAgoData
    val oldPoint2 = queue.peek()

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
  private var fieldDescriptor: FieldDescriptor? = null

  init {
    val fields = DataSnapshot.getDescriptor().fields
    for (field in fields) {
      val iRacingField = field.options.getExtension(LiveTelemetryPusherServiceOuterClass.iracingField)
      if (iRacingField == variableName) {
        fieldDescriptor = field
        break
      }
    }
    if (fieldDescriptor == null) {
      throw VariableNotFoundException(variableName)
    }
  }

  override val requiredOffset: Float = 0.0F

  override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
    val fieldValue = (telemetryDataPoint.dataSnapshot.getField(fieldDescriptor) as Number).toDouble()
    return DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, fieldValue)
  }
}

private class VariableNotFoundException(varName: String) : Exception("Variable $varName not found.")
