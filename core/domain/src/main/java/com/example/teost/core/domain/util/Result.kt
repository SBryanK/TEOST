package com.example.teost.util

sealed class Result<out T> {
    data object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}


