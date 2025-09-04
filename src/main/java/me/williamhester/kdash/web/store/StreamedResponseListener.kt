package me.williamhester.kdash.web.store

fun interface StreamedResponseListener<in T> {
  fun onNext(value: T)
}
