package com.afternote.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

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
        throw ApiException(code = code, message = message ?: "알 수 없는 서버 에러가 발생했습니다.")
    }
    return data ?: throw ApiException(code = code, message = "성공했으나 데이터가 비어있습니다.")
}

/**
 * 백엔드 응답이 status != 200 일 때 throw 되는 예외.
 *
 * [IOException] 의 서브클래스로 둔 이유 — OkHttp Interceptor (예: `ApiErrorInterceptor`)
 * 가 4xx/5xx 응답을 가로채 throw 할 때 OkHttp 가 그대로 전파할 수 있어야 하기 때문.
 * `Exception` 으로 두면 OkHttp 가 `IOException` 으로 래핑해 백엔드 message 가 손실된다.
 */
class ApiException(
    val code: Int,
    override val message: String,
) : IOException(message)

fun BaseResponse<*>.requireStatus() {
    if (status != 200) {
        throw ApiException(code = code, message = message ?: "요청에 실패했습니다.")
    }
}
