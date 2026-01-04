package me.williamhester.kdash.web.common

object Interpolation {
  fun interpolate(
    targetX: Float,
    lowX: Float,
    highX: Float,
    lowY: Float,
    highY: Float,
  ): Float =
    interpolate(targetX.toDouble(), lowX.toDouble(), highX.toDouble(), lowY.toDouble(), highY.toDouble()).toFloat()

  fun interpolate(
    targetX: Double,
    lowX: Double,
    highX: Double,
    lowY: Double,
    highY: Double,
  ): Double {
    val percentBetweenHighAndLow = (targetX - lowX) / (highX - lowX)
    val valueBetweenHighAndLow = percentBetweenHighAndLow * (highY - lowY)
    return valueBetweenHighAndLow + lowY
  }
}
