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
        throw ApiException(
            code = code,
            serverMessage = message,
            message = message ?: "알 수 없는 서버 에러가 발생했습니다.",
        )
    }
    return data ?: throw ApiException(
        code = code,
        serverMessage = null,
        message = "성공했으나 데이터가 비어있습니다.",
    )
}

/**
 * 백엔드 응답이 status != 200 일 때 throw 되는 예외.
 *
 * [IOException] 의 서브클래스로 둔 이유 — OkHttp Interceptor (예: `ApiErrorInterceptor`)
 * 가 4xx/5xx 응답을 가로채 throw 할 때 OkHttp 가 그대로 전파할 수 있어야 하기 때문.
 * `Exception` 으로 두면 OkHttp 가 `IOException` 으로 래핑해 백엔드 message 가 손실된다.
 *
 * @property serverMessage 서버가 실제로 내려준 사용자 친화 message. **null 이면 서버가 message 미제공**
 *   (4xx body 없거나 message blank). 호출처는 이 값이 null/non-null 인지로 "서버 message 있음/없음"
 *   을 판단 — `message` 는 클라 fallback 이 섞여 사용자에게 노출하면 안 됨.
 * @property message IOException 호환용 디버깅 메시지 (serverMessage 있으면 그것, 없으면 클라 fallback).
 *   사용자 직접 노출 X — Logcat·Crashlytics 용.
 */
class ApiException(
    val code: Int,
    val serverMessage: String?,
    override val message: String,
) : IOException(message)

fun BaseResponse<*>.requireStatus() {
    if (status != 200) {
        throw ApiException(
            code = code,
            serverMessage = message,
            message = message ?: "요청에 실패했습니다.",
        )
    }
}
