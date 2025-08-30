package me.williamhester.kdash.web.service.telemetrypusher

import com.google.common.flogger.FluentLogger
import me.williamhester.kdash.web.models.SessionKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class SessionConnectionRegistry {
  private val connectionIdsToCurrentSessionKey = ConcurrentHashMap<Int, SessionKey>()
  private val controllers = ConcurrentHashMap<SessionKey, SessionConnectionController>()

  /** Register a connection with the controller, for a given [SessionKey]. */
  fun register(connection: DriverConnection, sessionKey: SessionKey): LiveTelemetryDataWriter {
    val previousSessionInfo = connectionIdsToCurrentSessionKey[connection.connectionId]

    if (previousSessionInfo == sessionKey) {
      // Session info didn't change. Nothing to update.
      return controllers[sessionKey]!!.liveTelemetryDataWriter
    }
    if (previousSessionInfo != null) {
      // The connected driver's session changed. We need to update the session that it's connected to.
      controllers[previousSessionInfo]?.removeConnection(connection)
    }
    val controller = controllers.computeIfAbsent(sessionKey) { SessionConnectionController(sessionKey) }
    controller.addConnection(connection)
    return controller.liveTelemetryDataWriter
  }

  /** Notify the connections that this driver is currently on track. */
  fun driverOnTrack(connection: DriverConnection) {
    val currentSessionInfo = connectionIdsToCurrentSessionKey[connection.connectionId]
    if (currentSessionInfo == null) {
      logger.atWarning().log("Client is on track, but the session is not registered with the registry.")
      return
    }
    val controller = controllers[currentSessionInfo]
    if (controller == null) {
      logger.atWarning().log("Client is on track, but the controller for the session wasn't found.")
      return
    }
    controller.handleClientOnTrack(connection)
  }

  /** Unregister the connection and maybe tell one of the remaining connections to start sending data. */
  fun unregister(connection: DriverConnection) {
    val currentSessionInfo = connectionIdsToCurrentSessionKey[connection.connectionId]
    if (currentSessionInfo == null) {
      logger.atWarning().log("Client closed, but it was never registered.")
      return
    }
    val controller = controllers[currentSessionInfo]
    if (controller == null) {
      logger.atWarning().log("Client closed, but it was never registered.")
      return
    }
    controller.removeConnection(connection)
  }

  private class SessionConnectionController(sessionKey: SessionKey) {
    private val lock = ReentrantLock()
    private val connections = mutableSetOf<DriverConnection>()
    val liveTelemetryDataWriter = LiveTelemetryDataWriter(sessionKey)

    fun addConnection(connection: DriverConnection) = lock.withLock {
      connections.add(connection)
      ensureOneClientIsSendingData()
    }

    fun removeConnection(connection: DriverConnection) = lock.withLock {
      connections.removeIf { it.connectionId == connection.connectionId }
      ensureOneClientIsSendingData()
    }

    fun handleClientOnTrack(connection: DriverConnection) = lock.withLock {
      connections
        .filterNot { it.connectionId == connection.connectionId }
        .forEach(DriverConnection::requestClientStopsSendingData)
    }

    private fun ensureOneClientIsSendingData() = lock.withLock {
      val activeClient = connections.firstOrNull { it.isSendingData }
      if (activeClient != null) return

      val first = connections.firstOrNull()
      if (first == null) {
        logger.atWarning().log("No connected drivers.")
        return
      }
      first.requestClientStartsSendingData()
    }
  }

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}
