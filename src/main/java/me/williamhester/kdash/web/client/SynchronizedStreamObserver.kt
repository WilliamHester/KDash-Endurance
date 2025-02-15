package me.williamhester.kdash.web.client

import io.grpc.stub.StreamObserver

/**
 * StreamObserver that guarantees that [onNext] is called in a synchronzied way.
 *
 * StreamObserver by default is not thread safe. If [onNext] is called simultaneously, the proto serialization may
 * overwrite each other (partially), corrupting the data and/or dropping messages.
 */
internal class SynchronizedStreamObserver<T>(private val delegate: StreamObserver<T>) : StreamObserver<T> {
  override fun onNext(value: T) = synchronized(this) {
    delegate.onNext(value)
  }

  override fun onError(t: Throwable?) = delegate.onError(t)

  override fun onCompleted() = delegate.onCompleted()
}
