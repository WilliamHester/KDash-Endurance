package me.williamhester.kdash.web.state

import com.google.common.flogger.FluentLogger
import me.williamhester.kdash.enduranceweb.proto.DataSnapshot
import me.williamhester.kdash.web.state.DataSnapshotQueue.Companion.BUFFER_SECONDS
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max

/**
 * A queue of [me.williamhester.kdash.enduranceweb.proto.DataSnapshot] that buffers snapshots for [BUFFER_SECONDS] seconds before releasing them to a consumer.
 *
 * This allows data snapshots to be received out of order, in which case, they'll be placed in order and returned once
 * [BUFFER_SECONDS] seconds have passed.
 */
class DataSnapshotQueue : Iterator<DataSnapshot> {
  private var latestSnapshot = 0.0
  private val blockingPriorityQueue = PriorityBlockingQueue<DataSnapshot>(100) { left, right ->
    if (left.sessionTime - right.sessionTime > 0) 1 else -1
  }
  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  fun add(dataSnapshot: DataSnapshot) = lock.withLock {
    if (dataSnapshot.sessionTime < latestSnapshot - BUFFER_SECONDS) {
      logger.atWarning().log("DataSnapshot older than latest snapshot. Discarding.")
      return@withLock
    }

    latestSnapshot = max(dataSnapshot.sessionTime, latestSnapshot)
    blockingPriorityQueue.add(dataSnapshot)
    condition.signal()
  }

  override fun hasNext(): Boolean = true

  override fun next(): DataSnapshot {
    return lock.withLock {
      var oldestInQueue = blockingPriorityQueue.peek()
      while (oldestInQueue == null || oldestInQueue.sessionTime > latestSnapshot - BUFFER_SECONDS) {
        condition.await()
        oldestInQueue = blockingPriorityQueue.peek()
      }
      blockingPriorityQueue.take()
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()

    private const val BUFFER_SECONDS = 0.5
  }
}