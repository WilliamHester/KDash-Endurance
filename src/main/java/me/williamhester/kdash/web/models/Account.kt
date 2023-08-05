package me.williamhester.kdash.web.models

data class Account(
  val userId: Int,
  val username: String,
  val passwordHash: String,
  val firstName: String,
  val lastName: String,
)
