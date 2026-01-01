package me.williamhester.kdash.web.client

import com.google.common.flogger.FluentLogger
import me.williamhester.kdash.api.IRacingLoggedDataReader
import me.williamhester.kdash.api.VarBuffer
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class IRacingLoggedDataAndMetadataReader(
  ibtFilePath: Path,
  sessionInfoFilePath: Path,
) : IRacingLoggedDataReader(ibtFilePath) {
  private val fileReader = FileChannel.open(sessionInfoFilePath, StandardOpenOption.READ)
  private var nextHeader = readNextHeader()
  private var previousSessionStringBytes = ByteArray(0)
  private var mostRecentSessionTime = nextHeader.sessionTime

  override val metadata: SessionMetadata
    get() = parseMetadata()

  override fun hasNewMetadata(): Boolean = mostRecentSessionTime >= nextHeader.sessionTime

  override fun getMetadataBytes(): ByteArray {
    while (mostRecentSessionTime >= nextHeader.sessionTime) {
      val bytes = ByteArray(nextHeader.length)
      val byteBuffer = ByteBuffer.wrap(bytes)
      fileReader.read(byteBuffer)
      previousSessionStringBytes = bytes
      nextHeader = readNextHeader()
    }
    return previousSessionStringBytes
  }

  private fun readNextHeader(): Header {
    val byteBuffer = ByteBuffer.allocate(12)
    val bytesRead = fileReader.read(byteBuffer)
    if (bytesRead == -1) return Header(Double.MAX_VALUE, 0)
    byteBuffer.flip()
    return Header(byteBuffer.getDouble(), byteBuffer.getInt())
  }

  override fun next(): VarBuffer {
    val next = super.next()
    try {
      mostRecentSessionTime = next.getDouble("SessionTime")
    } catch (e: IndexOutOfBoundsException) {
      logger.atWarning().withCause(e).log("Out of bounds while reading session data")
    }
    return next
  }

  private data class Header(val sessionTime: Double, val length: Int)

  companion object {
    private val logger = FluentLogger.forEnclosingClass()
  }
}