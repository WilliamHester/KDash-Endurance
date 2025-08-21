package me.williamhester.kdash.web.query

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.LiveTelemetryPusherServiceOuterClass
import me.williamhester.kdash.web.models.TelemetryDataPoint

object VariableMapping {
  private val rawTelemetryFields: Map<String, (TelemetryDataPoint) -> Double>

  init {
    val fieldMap = mutableMapOf<String, (TelemetryDataPoint) -> Double>()
    val fields = DataSnapshot.getDescriptor().fields
    for (field in fields) {
      val iRacingField = field.options.getExtension(LiveTelemetryPusherServiceOuterClass.iracingField)
      fieldMap[iRacingField] = {
        telemetryDataPoint: TelemetryDataPoint ->
        (telemetryDataPoint.dataSnapshot.getField(field) as Number).toDouble()
      }
    }
    rawTelemetryFields = fieldMap
  }

  fun getGetter(fieldName: String): (TelemetryDataPoint) -> Double {
    return when (fieldName) {
      in rawTelemetryFields.keys -> rawTelemetryFields[fieldName]!!
      "LastPitLap" -> { tdp: TelemetryDataPoint -> tdp.syntheticFields.lastPitLap.toDouble() }
      "EstSpeed" -> { tdp: TelemetryDataPoint -> tdp.syntheticFields.estSpeed.toDouble() }
      "TrackPrecip" -> { tdp: TelemetryDataPoint -> tdp.syntheticFields.trackPrecip }
      else -> throw VariableNotFoundException(fieldName)
    }
  }
}

private class VariableNotFoundException(varName: String) : Exception("Variable $varName not found.")
