package com.example.teost.util

sealed class AppException(open val messageText: String? = null, open val causeThrowable: Throwable? = null) : Exception(messageText, causeThrowable) {
    data class ValidationException(override val messageText: String) : AppException(messageText)
    data class NetworkException(override val messageText: String? = null, override val causeThrowable: Throwable? = null) : AppException(messageText, causeThrowable)
    data class UnknownException(override val messageText: String? = null, override val causeThrowable: Throwable? = null) : AppException(messageText, causeThrowable)
}

fun Throwable.toAppException(): AppException = when (this) {
    is AppException -> this
    is java.net.SocketTimeoutException -> AppException.NetworkException("Connection timeout after 30 seconds", this)
    is java.net.UnknownHostException -> AppException.NetworkException("Host not found", this)
    is javax.net.ssl.SSLHandshakeException -> AppException.NetworkException("SSL handshake failed", this)
    is IllegalArgumentException -> AppException.ValidationException(message ?: "Invalid input")
    else -> AppException.UnknownException(message, this)
}


