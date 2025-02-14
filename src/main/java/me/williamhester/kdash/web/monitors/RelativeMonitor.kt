package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import java.util.concurrent.ConcurrentHashMap

/**
 * RelativeMonitor keeps track of the duration that a car was at the driver's car.
 */
class RelativeMonitor() {
  private val driverDistancesByTick = ConcurrentHashMap<Int, MutableList<Float>>()
  private val tickSeconds = mutableListOf<Double>()
  private var latestSeconds = 0.0
  private var numDrivers = 0

  fun process(dataSnapshot: DataSnapshot) {
    numDrivers = dataSnapshot.carIdxLapCompletedCount

    val latestSeconds = dataSnapshot.sessionTime
    tickSeconds.add(latestSeconds)
    this.latestSeconds = latestSeconds

    for (i in 0 until numDrivers) {
      val totalCompletedLaps = dataSnapshot.getCarIdxLapCompleted(i) + dataSnapshot.getCarIdxLapDistPct(i)
      driverDistancesByTick.computeIfAbsent(i) { mutableListOf() }.add(totalCompletedLaps)
    }
  }

  fun getGaps(): List<GapToCarId> {
    val sessionSeconds = latestSeconds
    val car0Distances = driverDistancesByTick[0]!!
    val car0CurrentDistance = car0Distances.last()

    val entries = mutableListOf<GapToCarId>()
    for (car in 0 until numDrivers) {
      val carDistances = driverDistancesByTick[car]!!
      val car1CurrentDistance = carDistances.last()
      val gap = when {
        car1CurrentDistance > car0CurrentDistance -> {
          val otherCarSeconds = findSecondsForDistance(car0CurrentDistance, carDistances)
          if (otherCarSeconds == null) null else sessionSeconds - otherCarSeconds
        }
        car0CurrentDistance > car1CurrentDistance -> {
          val otherCarSeconds = findSecondsForDistance(car1CurrentDistance, car0Distances)
          if (otherCarSeconds == null) null else otherCarSeconds - sessionSeconds
        }
        else -> 0.0
      }
      gap ?: continue
      entries.add(GapToCarId(gap, car))
    }
    entries.sortByDescending { it.gap }
    return entries
  }

  data class GapToCarId(val gap: Double, val carId: Int)

  private fun findSecondsForDistance(distance: Float, distances: List<Float>): Double? {
    if (distances.size < 2) return null
    var high = distances.size - 1
    var low = 0

    while (true) {
      val middle = (high + low) / 2
      val middleValue = distances[middle]
      when {
        distances[low] > distance -> return null
        (high - low) == 1 -> return interpolate(
          distance, distances[high], distances[low], tickSeconds[high], tickSeconds[low]
        )
        distance == middleValue -> return tickSeconds[middle]
        distance < middleValue -> high = middle
        else -> low = middle
      }
    }
  }

  private fun interpolate(
    targetDist: Float,
    highDist: Float,
    lowDist: Float,
    highSeconds: Double,
    lowSeconds: Double
  ): Double {
    val percentBetweenHighAndLow = (targetDist - lowDist) / (highDist - lowDist)
    val valueBetweenHighAndLow = percentBetweenHighAndLow * (highSeconds - lowSeconds)
    return valueBetweenHighAndLow + lowSeconds
  }
}
