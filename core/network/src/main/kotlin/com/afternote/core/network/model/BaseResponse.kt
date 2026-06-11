package com.afternote.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException

/**
 * 서버 공통 응답 봉투. 제네릭 [T] 는 `data` 필드의 페이로드 타입 — `data` 없는 엔드포인트는 `BaseResponse<Unit>`.
 *
 * 서버 스키마(`ApiResponse*`)에 더 있는 `expiresIn`(액세스 토큰 잔여 수명 초 — BE 2026-06-01 도입,
 * 클라의 만료 전 토큰 선제 갱신용이나 2026-06-11 실측 기준 라이브 무동작) 등 클라 미수신 필드는
 * 선언하지 않는다 — null 필드는 서버 직렬화에서 생략되고, `NetworkModule.provideJson` 의
 * ignoreUnknownKeys 가 선언 안 된 키를 무시한다. FE 가 토큰 선제 갱신을 구현하는 시점에 nullable 로 추가.
 */
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
 *
 * 헤더의 `IOException(message)` 는 부모 생성자 호출(Java 의 `super(message)`) — 인자는 주 생성자의
 * `message` 파라미터. 같은 값을 자기 프로퍼티(`String?` 인 Throwable.message 를 non-null 로 좁힘)와
 * 부모 Throwable 내부 필드 양쪽에 설정해, 디버거·Java 경로의 메시지 표시를 보존한다.
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
