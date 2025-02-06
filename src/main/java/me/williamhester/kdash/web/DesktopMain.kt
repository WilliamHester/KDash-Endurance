package me.williamhester.kdash.web

import me.williamhester.kdash.api.IRacingLiveDataReader

fun main() {
  val liveDataReader = IRacingLiveDataReader()
  ServerRunner(liveDataReader).run()
}
