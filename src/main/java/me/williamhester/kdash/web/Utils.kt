package me.williamhester.kdash.web

import com.google.protobuf.Message
import com.google.protobuf.Timestamp
import com.google.protobuf.util.JsonFormat
import io.javalin.http.Context
import java.time.Instant

fun validate(arg: Boolean, message: String) {
  if (!arg) {
    throw InvalidRequestException(message)
  }
}

fun Context.resultProtoAsJson(message: Message) {
  val sb = StringBuilder()
  JsonFormat.printer().includingDefaultValueFields().appendTo(message, sb)
  result(sb.toString())
}

fun Instant.toProtoTimestamp(): Timestamp {
  return Timestamp.newBuilder().apply {
    seconds = epochSecond
    nanos = nano
  }.build()
}
