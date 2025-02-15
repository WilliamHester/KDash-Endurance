package me.williamhester.kdash.web.state

import me.williamhester.kdash.enduranceweb.proto.SessionMetadata
import me.williamhester.kdash.enduranceweb.proto.sessionMetadata
import java.util.concurrent.CountDownLatch

class MetadataHolder {
  private val firstMetadataLatch = CountDownLatch(1)
  private var _metadata = sessionMetadata { }
  var metadata: SessionMetadata
    set(value) {
      firstMetadataLatch.countDown()
      _metadata = value
    }
    get() {
      firstMetadataLatch.await()
      return _metadata
    }
}
