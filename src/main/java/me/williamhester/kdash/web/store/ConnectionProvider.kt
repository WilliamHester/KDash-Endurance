package me.williamhester.kdash.web.store

import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.util.Scanner

object ConnectionProvider {
  fun get(): Connection {
    val password: String
    Files.newInputStream(Paths.get("main/java/me/williamhester/kdash/web/store/db.password")).use {
      password = Scanner(it).next()
    }
    return DriverManager.getConnection("jdbc:mysql://localhost/kdash", "root", password)
  }
}