package com.example.teost.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

object ErrorMapper {
    fun toAppError(e: Exception): AppError = when (e) {
        is SocketTimeoutException -> AppError.Network("Request timed out")
        is UnknownHostException -> AppError.Network("Host not found")
        is SSLHandshakeException -> AppError.SecurityTest("SSL handshake failed")
        // Treat IllegalArgumentException as validation but with gentle message, avoids noisy tech details
        is IllegalArgumentException -> AppError.Validation("Invalid input")
        is IOException -> AppError.Network(e.message)
        else -> classifyByPackage(e)
    }

    private fun classifyByPackage(e: Exception): AppError {
        val name = e.javaClass.name
        return when {
            name.startsWith("com.google.firebase.auth") -> AppError.Authentication(e.message)
            name.startsWith("com.google.firebase.firestore") -> AppError.Database(e.message)
            name.startsWith("com.google.firebase") -> AppError.Network(e.message)
            else -> AppError.Unknown(e.message)
        }
    }
}


