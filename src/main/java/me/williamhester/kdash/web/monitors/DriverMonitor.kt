package me.williamhester.kdash.web.monitors

import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.state.MetadataHolder

class DriverMonitor(
  private val metadataHolder: MetadataHolder,
) {
  val currentDrivers: Map<Int, CarInfo>
    get() {
      val currentMetadata = metadataHolder.metadata
      val driverList = currentMetadata["DriverInfo"]["Drivers"].listList
      val paceCarIdx = currentMetadata["DriverInfo"]["PaceCarIdx"].value.toInt()

      return driverList
        .map {
          Pair(
            it["CarIdx"].value.toInt(),
            CarInfo(
              carNumber = it["CarNumberRaw"].value.toInt(),
              carClassId = it["CarClassID"].value.toInt(),
              carClassName = it["CarClassShortName"].value,
              driverName = it["UserName"].value,
              teamName = it["TeamName"].value,
            )
          )
        }
        .filter { it.first != paceCarIdx }
        .associateBy({ it.first }, { it.second })
    }

  data class CarInfo(
    val carNumber: Int,
    val carClassId: Int,
    val carClassName: String,
    val driverName: String,
    val teamName: String,
  )
}