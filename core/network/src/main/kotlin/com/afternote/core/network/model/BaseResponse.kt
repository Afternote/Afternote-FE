package com.afternote.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    @SerialName("status")
    val status: Int,
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String? = null,
    @SerialName("data")
    val data: T? = null,
)

fun <T : Any> BaseResponse<T>.requireData(): T {
    if (status != 200) {
        throw ApiException(
            status = status,
            code = code,
            message = message ?: "알 수 없는 서버 에러가 발생했습니다.",
        )
    }
    return data ?: throw ApiException(
        status = status,
        code = code,
        message = "성공했으나 데이터가 비어있습니다.",
    )
}

class ApiException(
    val status: Int,
    val code: Int,
    override val message: String,
) : Exception(message)

fun BaseResponse<*>.requireStatus() {
    if (status != 200) {
        throw ApiException(
            status = status,
            code = code,
            message = message ?: "요청에 실패했습니다.",
        )
    }
}
