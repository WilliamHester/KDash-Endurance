package me.williamhester.kdash.web.service.telemetrypusher.monitors

import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.enduranceweb.proto.lookupTable
import me.williamhester.kdash.enduranceweb.proto.lookupTables
import me.williamhester.kdash.web.extensions.ProtoExtensions.toIterable
import me.williamhester.kdash.web.extensions.get
import me.williamhester.kdash.web.service.telemetrypusher.state.MetadataHolder
import me.williamhester.kdash.web.store.SessionStore

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
    val maxEstLapTimes = mutableListOf(lapLength)
    for (driverIdx in driverIdxs) {
      maxEstLapTimes += processDriverIdxDataSnapshot(driverIdx, dataSnapshot)
    }
    lapLength = maxEstLapTimes.max()

    // Only update this every half lap. This keeps the (amortized) size of the updates small, and we only really need
    // updates once every lap or so. Twice per lap should be more than enough.
    if (dataSnapshot.sessionTime > lastLogTime + lapLength / 2) {
      storeLookupTables(driverIdxs)
      lastLogTime = dataSnapshot.sessionTime
    }
  }

  private fun processDriverIdxDataSnapshot(
    driverIdx: Int,
    dataSnapshot: DataSnapshot
  ): Float {
    val monitor = carIdxDistanceLookupTableMonitors.computeIfAbsent(driverIdx) {
      CarIdxDistanceLookupTableMonitor(metadataHolder, it)
    }
    return monitor.process(dataSnapshot)
  }

  private fun storeLookupTables(driverIdxs: List<Int>) {
    val lookupTables = lookupTables {
      for (driverIdx in driverIdxs) {
        carIdxEstTimeToDistance[driverIdx] = carIdxDistanceLookupTableMonitors[driverIdx]!!.getLookupTable()
      }
      driverCarDistanceMetersToEstTime = lookupTable {
        values += relativeMonitor2.driverCarEstTimeAtDistance.toIterable()
      }
    }
    sessionStore.insertCarIdxLookupTables(lookupTables)
  }
}
