package me.williamhester.kdash.web.service.telemetrypusher.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.lookupTable
import me.williamhester.kdash.enduranceweb.proto.lookupTables
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.service.telemetrypusher.state.MetadataHolder
import me.williamhester.kdash.web.store.SessionStore
import kotlin.math.max

class DistanceLookupTableMonitor(
  private val metadataHolder: MetadataHolder,
  private val sessionStore: SessionStore,
  private val relativeMonitor2: RelativeMonitor2,
) {
  private val carIdxDistanceLookupTableMonitors = mutableMapOf<Int, CarIdxDistanceLookupTableMonitor>()
  private var lastLogTime = 0.0
  private var lapLength = 60.0F

  fun process(dataSnapshot: DataSnapshot) {
    val driverIdxs = metadataHolder.metadata["DriverInfo"]["Drivers"].listList.map { it["CarIdx"].value.toInt() }
    var maxEstLapTime = lapLength
    for (driverIdx in driverIdxs) {
      val monitor = carIdxDistanceLookupTableMonitors.computeIfAbsent(driverIdx) {
        CarIdxDistanceLookupTableMonitor(metadataHolder, it)
      }
      maxEstLapTime = max(monitor.process(dataSnapshot), maxEstLapTime)
    }
    lapLength = max(lapLength, maxEstLapTime)

    // Only update this every half lap. This keeps the (amortized) size of the updates small, and we only really need
    // updates once every lap or so. Twice per lap should be more than enough.
    if (dataSnapshot.sessionTime > lastLogTime + lapLength / 2) {
      lastLogTime = dataSnapshot.sessionTime
      val lookupTables = lookupTables {
        for (driverIdx in driverIdxs) {
          carIdxEstTimeToDistance[driverIdx] = carIdxDistanceLookupTableMonitors[driverIdx]!!.getLookupTable()
        }
        driverCarDistanceMetersToEstTime = lookupTable {
          for (i in 0..<relativeMonitor2.driverCarEstTimeAtDistance.size) {
            values += relativeMonitor2.driverCarEstTimeAtDistance[i]
          }
        }
      }
      sessionStore.insertCarIdxLookupTables(lookupTables)
    }
  }
}
