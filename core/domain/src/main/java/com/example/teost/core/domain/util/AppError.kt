package com.example.teost.util

sealed class AppError(open val message: String? = null) {
    data class Network(override val message: String? = null) : AppError(message)
    data class Authentication(override val message: String? = null) : AppError(message)
    data class Validation(override val message: String? = null) : AppError(message)
    data class Database(override val message: String? = null) : AppError(message)
    data class SecurityTest(override val message: String? = null) : AppError(message)
    data class Unknown(override val message: String? = null) : AppError(message)
}


