package me.williamhester.kdash.web

/** The base type for all exceptions that should be thrown during request handling. */
sealed class RequestException(val status: Int, message: String) : Exception(message)

/** The request was invalid for some reason. */
class InvalidRequestException(message: String) : RequestException(400, message)

/** The request was not authenticated. */
class UnauthenticatedException() : RequestException(403, "Not authenticated")
