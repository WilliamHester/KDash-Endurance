package me.williamhester.kdash.web.query

import com.google.common.truth.Truth.assertThat
import me.williamhester.kdash.enduranceweb.proto.dataSnapshot
import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.TelemetryDataPoint
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProcessorsTest {
  @Test
  fun variableProcessor_validVariable_getsData() {
    val telemetryDataPoint = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1,
      driverDistance = DRIVER_DISTANCE_1,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1
      }
    )

    val processor = VariableProcessor("FuelLevel")

    val result = processor.process(telemetryDataPoint)

    assertThat(result).isEqualTo(DataPoint(SESSION_TIME_1, DRIVER_DISTANCE_1, FUEL_LEVEL_1.toDouble()))
  }

  @Test
  fun lapDeltaProcessor_returnsZeroIfNotEnoughDataSeen() {
    val telemetryDataPoint = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1,
      driverDistance = DRIVER_DISTANCE_1,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1
      }
    )

    val processor = LapDeltaProcessor(VariableProcessor("FuelLevel"))

    val result = processor.process(telemetryDataPoint)

    assertThat(result).isEqualTo(DataPoint(SESSION_TIME_1, DRIVER_DISTANCE_1, 0.0))
  }

  @Test
  fun lapDeltaProcessor_returnsDeltaBetweenTwoPoints() {
    val telemetryDataPoint1 = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1,
      driverDistance = DRIVER_DISTANCE_1,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1
      }
    )
    val telemetryDataPoint2 = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1 + 1,
      driverDistance = DRIVER_DISTANCE_1 + 0.0001F,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1 - 4.0F
      }
    )
    val telemetryDataPoint3 = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1 + 2,
      driverDistance = DRIVER_DISTANCE_1 + 1F,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1 - 4.0F
      }
    )

    val processor = LapDeltaProcessor(VariableProcessor("FuelLevel"))

    processor.process(telemetryDataPoint1)
    processor.process(telemetryDataPoint2)
    val result = processor.process(telemetryDataPoint3)

    assertThat(result.value).isWithin(0.001).of(4.0)
  }

  @Test
  fun lapDeltaProcessor_interpolates() {
    val telemetryDataPoint1 = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1,
      driverDistance = DRIVER_DISTANCE_1 - 0.1F,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1 + 0.25F
      }
    )
    val telemetryDataPoint2 = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1 + 1,
      driverDistance = DRIVER_DISTANCE_1 + 0.3F,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1 - 0.75F
      }
    )
    val telemetryDataPoint3 = TelemetryDataPoint(
      sessionTime = SESSION_TIME_1 + 2,
      driverDistance = DRIVER_DISTANCE_1 + 1F,
      dataSnapshot = dataSnapshot {
        fuelLevel = FUEL_LEVEL_1 - 4.0F
      }
    )

    val processor = LapDeltaProcessor(VariableProcessor("FuelLevel"))

    processor.process(telemetryDataPoint1)
    processor.process(telemetryDataPoint2)
    val result = processor.process(telemetryDataPoint3)

    assertThat(result.value).isWithin(0.001).of(4.0)
  }

  companion object {
    private const val SESSION_TIME_1 = 1.0
    private const val DRIVER_DISTANCE_1 = 5.0F
    private const val FUEL_LEVEL_1 = 75.0F
  }
}