package me.williamhester.kdash.web.store

import java.sql.Connection
import java.sql.DriverManager

object ConnectionProvider {
  fun get(): Connection {
    return DriverManager.getConnection("jdbc:postgresql://localhost:5432/williamhester")
  }
}