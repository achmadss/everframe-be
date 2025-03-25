package dev.achmad.common.model

data class BaseResponse<T>(
    val statusCode: Int,
    val errorCode: Int? = null,
    val message: String? = null,
    val data: T?
)