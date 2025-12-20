package me.williamhester.kdash.web.query

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceOuterClass
import me.williamhester.kdash.web.models.DataPointValue
import me.williamhester.kdash.web.models.ListValue
import me.williamhester.kdash.web.models.ScalarValue
import me.williamhester.kdash.web.models.TelemetryDataPoint

object VariableMapping {
  private val rawTelemetryFields: Map<String, (TelemetryDataPoint) -> DataPointValue>

  init {
    val fieldMap = mutableMapOf<String, (TelemetryDataPoint) -> DataPointValue>()
    val fields = DataSnapshot.getDescriptor().fields
    for (field in fields) {
      val iRacingField = field.options.getExtension(LiveTelemetryPusherServiceOuterClass.iracingField)
      fieldMap[iRacingField] = { telemetryDataPoint: TelemetryDataPoint ->
        if (field.isRepeated) {
          val count = telemetryDataPoint.dataSnapshot.getRepeatedFieldCount(field)
          val list = List(count) { (telemetryDataPoint.dataSnapshot.getRepeatedField(field, it) as Number).toDouble() }
          ListValue(list)
        } else {
          ScalarValue((telemetryDataPoint.dataSnapshot.getField(field) as Number).toDouble())
        }
      }
    }
    rawTelemetryFields = fieldMap
  }

  fun getGetter(fieldName: String): (TelemetryDataPoint) -> DataPointValue {
    return when (fieldName) {
      in rawTelemetryFields.keys -> rawTelemetryFields[fieldName]!!
      "LastPitLap" -> { tdp: TelemetryDataPoint -> ScalarValue(tdp.syntheticFields.lastPitLap) }
      "EstSpeed" -> { tdp: TelemetryDataPoint -> ScalarValue(tdp.syntheticFields.estSpeed) }
      "TrackPrecip" -> { tdp: TelemetryDataPoint -> ScalarValue(tdp.syntheticFields.trackPrecip) }
      "PitOptRepairRemaining" -> { tdp: TelemetryDataPoint -> ScalarValue(tdp.syntheticFields.optionalRepairsRemaining) }
      "PitReqRepairRemaining" -> { tdp: TelemetryDataPoint -> ScalarValue(tdp.syntheticFields.requiredRepairsRemaining) }
      "LapFuelUsed" -> { tdp: TelemetryDataPoint -> ScalarValue(tdp.syntheticFields.lapFuelUsed) }
      else -> throw VariableNotFoundException(fieldName)
    }
  }
}

private class VariableNotFoundException(varName: String) : Exception("Variable $varName not found.")
