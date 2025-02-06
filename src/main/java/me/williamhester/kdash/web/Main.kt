package me.williamhester.kdash.web

import me.williamhester.kdash.api.IRacingLoggedDataReader
import java.nio.file.Paths

fun main() {
  ServerRunner(IRacingLoggedDataReader(Paths.get("/Users/williamhester/Downloads/livedata.ibt"))).run()
}
