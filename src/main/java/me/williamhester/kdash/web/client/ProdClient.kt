package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import io.grpc.ConnectivityState
import io.grpc.netty.NettyChannelBuilder
import me.williamhester.kdash.api.IRacingLiveDataReader

internal object ProdClient {

  private val logger = FluentLogger.forEnclosingClass()
  private const val DOMAIN = "ifgapcar.racing"

  @JvmStatic
  fun main(args: Array<String>) {
    Thread.setDefaultUncaughtExceptionHandler {
        _, e ->
      logger.atSevere().withCause(e).log("Uncaught exception")
    }
    val channel = NettyChannelBuilder.forTarget(DOMAIN).build()
    channel.getState(true)
    while (true) {
      val state = channel.getState(false)
      when (state) {
        ConnectivityState.CONNECTING -> {
          logger.atInfo().log("Connecting to %s", DOMAIN)
        }
        ConnectivityState.READY -> {
          logger.atInfo().log("Connected to %s", DOMAIN)
          break
        }
        ConnectivityState.TRANSIENT_FAILURE -> {
          logger.atInfo().log("Transient failure when connecting to %s", DOMAIN)
        }
        ConnectivityState.IDLE -> {
          logger.atInfo().log("Connection idle")
        }
        ConnectivityState.SHUTDOWN -> {
          logger.atInfo().log("Connection shut down")
        }
        else -> {
          logger.atInfo().log("Connection null")
        }
      }
      Thread.sleep(200)
    }
    Client(
      IRacingLiveDataReader(),
      channel,
    ).connect()
    Thread.sleep(365 * 24 * 60 * 60 * 1000L)
  }
}
