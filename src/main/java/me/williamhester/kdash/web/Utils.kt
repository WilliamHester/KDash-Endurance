package me.williamhester.kdash.web

fun validate(arg: Boolean, message: String) {
  if (!arg) {
    throw InvalidRequestException(message)
  }
}
