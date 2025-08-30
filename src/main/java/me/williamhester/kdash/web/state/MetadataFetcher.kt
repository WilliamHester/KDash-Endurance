package me.williamhester.kdash.web.state

import com.google.common.base.Stopwatch
import me.williamhester.kdash.web.models.SessionKey
import me.williamhester.kdash.web.store.Store
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class MetadataFetcher(private val sessionKey: SessionKey) {
  val metadata by cached(Duration.ofSeconds(1)) {
    Store.getMetadataForSession(sessionKey)!!
  }
}

private fun <T, V> cached(
  expiration: Duration,
  fetcher: () -> V,
) : ReadOnlyProperty<T, V> {
  return object : ReadOnlyProperty<T, V> {
    private val stopwatch = Stopwatch.createStarted()
    @Volatile
    private var current: V? = null
    private val updateLock = Any()

    override fun getValue(thisRef: T, property: KProperty<*>): V {
      if (current == null || stopwatch.elapsed(TimeUnit.MILLISECONDS) > expiration.toMillis()) {
        synchronized(updateLock) {
          if (current == null || stopwatch.elapsed(TimeUnit.MILLISECONDS) > expiration.toMillis()) {
            current = fetcher()
            stopwatch.reset().start()
          }
        }
      }
      return current!!
    }
  }
}
