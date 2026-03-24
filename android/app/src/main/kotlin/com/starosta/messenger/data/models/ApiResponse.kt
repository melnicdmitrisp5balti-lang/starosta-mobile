package com.starosta.messenger.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
)

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = 0) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
