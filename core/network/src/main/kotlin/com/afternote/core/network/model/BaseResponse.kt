package com.afternote.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버 공통 응답 봉투. 제네릭 [T] 는 `data` 필드의 페이로드 타입 — `data` 없는 엔드포인트는 `BaseResponse<Unit>`.
 *
 * 액세스 토큰 잔여 수명(`expiresIn`)은 BE #410(2026-06-20)으로 발급 응답의 `data`(`LoginDto`·
 * `ReissueDto`) 안으로 옮겨졌다 — 더 이상 봉투 최상위 필드가 아니다. 그 외 서버 스키마
 * (`ApiResponse*`)의 클라 미소비 필드는 선언하지 않는다 (`NetworkModule.provideJson` 의
 * ignoreUnknownKeys 가 무시).
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

/**
 * 봉투 계약상 성공 대역.
 *
 * `status` 에는 BE 가 HTTP 상태를 그대로 실어 보낸다 — 성공은 `ApiResponse.success` 의
 * `HttpStatus.OK.value()`, 실패는 `ErrorCode.getHttpStatus().value()`. 별도 도메인 코드가 아니라
 * HTTP 상태 코드이므로 성공 판정도 HTTP 의미론과 같은 2xx 대역으로 한다. `ApiErrorCallAdapterFactory`
 * 가 HTTP 201·202·204 를 성공으로 통과시키는 것과 기준을 맞춘 것이며, 200 한 점으로 좁히면 BE 가
 * 201·204 를 쓰기 시작할 때 성공 응답이 봉투 단계에서 조용히 실패로 뒤집힌다.
 */
private val BaseResponse<*>.isSuccess: Boolean
    get() = status in 200..299

fun <T : Any> BaseResponse<T>.requireData(): T {
    throwIfEnvelopeFailed(fallbackMessage = "알 수 없는 서버 에러가 발생했습니다.")
    return data ?: throw ApiException(
        // 봉투는 성공(2xx)이라 했는데 payload 가 비었다 — 계약 위반이므로 status 는 봉투 값 그대로 남긴다.
        status = status,
        code = code,
        serverMessage = null,
        fallbackMessage = "성공했으나 데이터가 비어있습니다.",
    )
}

/**
 * 서버가 응답을 마쳤지만 HTTP 또는 공통 응답 봉투 계약이 실패했을 때 throw 되는 예외.
 *
 * HTTP 400..599는 Retrofit CallAdapter가 만들고, HTTP 2xx 응답 뒤에는 [requireData]·[requireStatus]가
 * 봉투 `status` 가 성공 대역(2xx) 밖이거나 필수 `data` 가 누락된 경우를 이 타입으로 올린다. 모두 서버 응답을 받은 뒤의
 * 내용 실패이므로 전송 계층 실패인 `IOException`과 타입 계층을 공유하지 않는다.
 *
 * @property status HTTP 400..599 경로에서는 HTTP 상태, 2xx 본문 검증 경로에서는
 *   [BaseResponse.status]. 서버가 4xx·5xx 응답 모두에 `message`를 실어 보내므로(실측: 500 응답 body에
 *   내부 SQL 문구 — #511), [serverMessage] 유무만으로는 "서버가 예상하고 처리한 사용자 오류"와
 *   "장애"를 가를 수 없다. 그 판정이 필요한 호출처는 이 값의 대역을 본다.
 * @property serverMessage 서버가 실제로 내려준 원문 message. **null 이면 서버가 message 미제공**
 *   (4xx body 없거나 message blank). 값이 있어도 곧바로 사용자 노출 가능하다는 뜻은 아니며, 호출처가
 *   status·code를 함께 판단한다. [Throwable.message]에는 클라이언트 fallback도 섞일 수 있다.
 * [Throwable.message]는 [serverMessage]가 있으면 그 값을, 없으면 [fallbackMessage]를 사용한다.
 * 사용자 직접 노출 X — Logcat·Crashlytics 용.
 * @param fallbackMessage 서버 message가 없을 때 [Throwable.message]로 사용할 클라이언트 진단 문구.
 */
class ApiException(
    val status: Int,
    val code: Int,
    val serverMessage: String?,
    fallbackMessage: String,
) : RuntimeException(serverMessage ?: fallbackMessage)

fun BaseResponse<*>.requireStatus() {
    throwIfEnvelopeFailed(fallbackMessage = "요청에 실패했습니다.")
}

/**
 * 봉투가 실패를 말하면([isSuccess] 가 false) [ApiException] 으로 올린다.
 *
 * [fallbackMessage] 를 인자로 받는 이유 — 이 문구는 서버가 `message` 를 주지 않았을 때만
 * [Throwable.message] 로 쓰이는 진단 문구이고(사용자 직접 노출 X), 실패가 payload 를 요구한
 * 호출([requireData])에서 왔는지 성공 여부만 보는 호출([requireStatus])에서 왔는지를 가르는
 * 단서다. 하나로 합치면 그 구분이 사라지므로 호출처가 각자의 문구를 넘긴다.
 */
private fun BaseResponse<*>.throwIfEnvelopeFailed(fallbackMessage: String) {
    if (!isSuccess) {
        throw ApiException(
            status = status,
            code = code,
            serverMessage = message,
            fallbackMessage = fallbackMessage,
        )
    }
}
