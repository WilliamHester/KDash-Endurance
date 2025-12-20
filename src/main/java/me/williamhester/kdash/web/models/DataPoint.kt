package me.williamhester.kdash.web.models

data class DataPoint(
  val sessionTime: Double,
  val driverDistance: Float,
  val value: DataPointValue,
) {
  constructor(
    sessionTime: Double,
    driverDistance: Float,
    value: Double,
  ) : this(sessionTime, driverDistance, ScalarValue(value))
}

sealed interface DataPointValue {
  private fun operator(other: DataPointValue, op: (Double, Double) -> Double): DataPointValue {
    return when {
      this is ScalarValue && other is ScalarValue -> ScalarValue(op(this.value, other.value))
      this is ListValue && other is ListValue -> ListValue(this.value.zip(other.value).map { op(it.first, it.second) })
      this is ScalarValue && other is ListValue -> ListValue(other.value.map { op(this.value, it) })
      this is ListValue && other is ScalarValue -> ListValue(this.value.map { op(it, other.value) })
      else -> throw AssertionError("Impossible")
    }
  }

  operator fun plus(other: DataPointValue): DataPointValue = operator(other, Double::plus)

  operator fun minus(other: DataPointValue): DataPointValue = operator(other, Double::minus)

  operator fun times(other: DataPointValue): DataPointValue = operator(other, Double::times)

  operator fun div(other: DataPointValue): DataPointValue = operator(other, Double::div)

  operator fun div(other: Number): DataPointValue {
    return when (this) {
      is ScalarValue -> ScalarValue(this.value / other.toDouble())
      is ListValue -> ListValue(this.value.map { it / other.toDouble() })
    }
  }

  operator fun compareTo(other: DataPointValue): Int {
    return when {
      this is ScalarValue && other is ScalarValue -> {
        when {
          this.value > other.value -> 1
          this.value == other.value -> 0
          this.value < other.value -> -1
          else -> throw AssertionError()
        }
      }
      else -> 0
    }
  }

  class IncompatibleOperandsException : Exception()
}

class ScalarValue(val value: Double) : DataPointValue {
  constructor(number: Number) : this(number.toDouble())
}

class ListValue(val value: List<Double>) : DataPointValue

object DataPointValues {
  operator fun Double.plus(dataPointValue: DataPointValue): DataPointValue {
    return when (dataPointValue) {
      is ScalarValue -> ScalarValue(this + dataPointValue.value)
      is ListValue -> ListValue(dataPointValue.value.map { it + this })
    }
  }

  operator fun Double.times(dataPointValue: DataPointValue): DataPointValue {
    return when (dataPointValue) {
      is ScalarValue -> ScalarValue(this * dataPointValue.value)
      is ListValue -> ListValue(dataPointValue.value.map { it * this })
    }
  }
}