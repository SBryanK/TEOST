package com.example.teost.util

sealed class Resource<T>(
    val data: T? = null,
    val error: AppError? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data = data)
    class Error<T>(error: AppError, message: String? = error.message, data: T? = null) : Resource<T>(data = data, error = error, message = message)
    class Loading<T>(data: T? = null) : Resource<T>(data = data)
}
