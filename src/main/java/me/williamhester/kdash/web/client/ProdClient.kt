package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import com.google.common.util.concurrent.RateLimiter
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import me.williamhester.kdash.api.IRacingLiveDataReader
import java.util.concurrent.TimeUnit

internal object ProdClient {

  private val logger = FluentLogger.forEnclosingClass()
  private const val DOMAIN = "ifgapcar.racing"

  @JvmStatic
  fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler {
        _, e ->
      logger.atSevere().withCause(e).log("Uncaught exception")
    }
    val reader = createDataReader()
    val channel = createAndConnectToChannel()
    Client(reader, channel).connect()
    Thread.sleep(365 * 24 * 60 * 60 * 1000L)
  }

  private fun createAndConnectToChannel(): ManagedChannel {
    val rateLimiter = RateLimiter.create(5.0)
    val channel = NettyChannelBuilder.forTarget(DOMAIN).build()
    channel.getState(true)
    while (true) {
      rateLimiter.acquire()
      val state = channel.getState(false)
      when (state) {
        ConnectivityState.CONNECTING -> logger.atInfo().log("Connecting to %s", DOMAIN)
        ConnectivityState.READY -> {
          logger.atInfo().log("Connected to %s", DOMAIN)
          break
        }
        ConnectivityState.TRANSIENT_FAILURE -> logger.atInfo().log("Transient failure when connecting to %s", DOMAIN)
        ConnectivityState.IDLE -> logger.atInfo().log("Connection idle")
        ConnectivityState.SHUTDOWN -> logger.atInfo().log("Connection shut down")
        else -> logger.atInfo().log("Connection null")
      }
    }
    return channel
  }

  private fun createDataReader(): IRacingLiveDataReader {
    val rateLimiter = RateLimiter.create(1.0)
    while (true) {
      rateLimiter.acquire()
      try {
        val reader = IRacingLiveDataReader()
        logger.atInfo().log("Connected to iRacing live data!")
        return reader
      } catch (e: Exception) {
        logger.atWarning()
          .atMostEvery(10, TimeUnit.SECONDS)
          .log("Error while creating live data reader. Is the sim open?")
        continue
      }
    }
  }
}
