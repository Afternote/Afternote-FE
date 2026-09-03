package com.afternote.feature.receiver.data.error

import com.afternote.core.network.model.ApiException
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import java.io.IOException

/** BE `ErrorCode.DELIVERY_CONDITION_NOT_MET(HttpStatus.FORBIDDEN, 2009, "아직 전달 조건이 충족되지 않았습니다.")`. */
private const val DELIVERY_CONDITION_NOT_MET = 2009

/**
 * 인프라 예외를 수신자 도메인 실패로 옮긴다. [ApiException]은 서버 응답 여부가 확인됐으므로 전부
 * 도메인 실패로 번역하고, 그 밖의 분류 대상이 아닌 실패만 원본 그대로 돌려준다.
 *
 * 서버가 응답하며 거절한 [ApiException]과 서버에 닿지 못한 [IOException]을 별도 도메인 타입으로
 * 옮긴다. 둘은 Retrofit CallAdapter 경계부터 서로 다른 타입 계열이다.
 *
 * 취소는 여기 오지 않는다 — 호출처의 `runCatchingCancellable` 이 `CancellationException` 을
 * 먼저 되던진다.
 */
internal fun Throwable.toReceiverFailure(): Throwable =
    when (this) {
        is ApiException -> toReceiverServerFailure()
        is IOException -> ReceiverFailure.NetworkUnavailable(this)
        else -> this
    }

/**
 * 요청 결과의 실패를 [toReceiverFailure] 로 옮긴다 — **수신자 저장소가 모듈 밖으로 실패를 내보내는
 * 유일한 통로**다.
 *
 * 메서드마다 `try { ... } catch (e: ApiException)` 을 포개던 자리를 대신한다. 그 형태는 감싼 메서드만
 * 번역해서, 같은 모듈 안에서 어떤 endpoint 는 도메인 어휘로 나가고 어떤 endpoint 는 `ApiException`
 * (status·BE code·서버 message)을 그대로 흘리는 상태를 만들었다. 호출부가 한 줄로 붙이는 형태여야
 * 새 endpoint 가 번역을 빠뜨리지 않는다.
 *
 * 번역 대상이 아닌 실패는 **같은 [Result] 인스턴스를 그대로** 돌려준다 — 매핑 실패
 * ([com.afternote.feature.receiver.data.mapper.ReceiverListMappingFailure] 등) 처럼 도메인 어휘가 없는
 * 실패를 새 [Result] 로 갈아 끼우지 않는다.
 *
 * 취소는 여기 오지 않는다 — 호출부의 `runCatchingCancellable` 이 [Result] 에 담지 않고 되던진다.
 */
internal fun <T> Result<T>.mapReceiverFailure(): Result<T> {
    val original = exceptionOrNull() ?: return this
    val translated = original.toReceiverFailure()
    return if (translated === original) this else Result.failure(translated)
}

/**
 * 서버 응답의 status·code·message 를 도메인 어휘로 번역한다. 판정 순서 —
 *
 * 1. 4xx 대역이 아니면 [ReceiverFailure.UnexpectedServerFailure]. 등재 code 여도 5xx 봉투는 장애다.
 * 2. FE 가 등재한 code 는 **code 만으로** 사유가 확정된다 — 2009 는
 *    [ReceiverFailure.DeliveryConditionNotMet], 나머지 5개는 [ReceiverRejectionReason] 을 채운
 *    [ReceiverFailure.UserRejection]. 이 분기의 화면 문구는 로컬 리소스라 서버 문구를 쓰지 않으므로,
 *    서버가 문구를 비워 보내도 확정 사유가 유지된다(#1339 리뷰).
 * 3. 미등재 code 만 서버 문구 게이트를 지난다. 문구가 있으면 서버가 예상하고 처리한 사용자 거절로
 *    보아 `reason = null` 인 [ReceiverFailure.UserRejection](리포팅 제외, 서버 원문 미노출), 문구까지
 *    없으면 계약 불일치 신호로 [ReceiverFailure.UnexpectedServerFailure] 로 기록한다.
 *
 * **BE `ErrorCode` 번호를 아는 것은 이 계층까지다.** 도메인 실패는 status·code·message 어느 것도
 * 운반하지 않으며, presentation 은 번역된 타입과 사유만 소비한다.
 */
private fun ApiException.toReceiverServerFailure(): ReceiverFailure {
    val registeredReason = code.toReceiverRejectionReasonOrNull()
    return when {
        status !in CLIENT_ERROR_STATUS_RANGE -> {
            ReceiverFailure.UnexpectedServerFailure(this)
        }

        code == DELIVERY_CONDITION_NOT_MET -> {
            ReceiverFailure.DeliveryConditionNotMet(this)
        }

        registeredReason != null -> {
            ReceiverFailure.UserRejection(reason = registeredReason, cause = this)
        }

        !serverMessage.isNullOrBlank() -> {
            ReceiverFailure.UserRejection(reason = null, cause = this)
        }

        else -> {
            ReceiverFailure.UnexpectedServerFailure(this)
        }
    }
}

private fun Int.toReceiverRejectionReasonOrNull(): ReceiverRejectionReason? =
    when (this) {
        INVALID_AUTH_CODE -> ReceiverRejectionReason.INVALID_AUTH_CODE
        RECEIVER_EMAIL_NOT_FOUND -> ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND
        RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND -> ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND
        RECEIVER_EMAIL_AUTH_CODE_MISMATCH -> ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_MISMATCH
        VERIFICATION_ALREADY_SUBMITTED -> ReceiverRejectionReason.VERIFICATION_ALREADY_SUBMITTED
        else -> null
    }

private val CLIENT_ERROR_STATUS_RANGE = 400..499

private const val INVALID_AUTH_CODE = 1900
private const val RECEIVER_EMAIL_NOT_FOUND = 1901
private const val RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND = 1902
private const val RECEIVER_EMAIL_AUTH_CODE_MISMATCH = 1903
private const val VERIFICATION_ALREADY_SUBMITTED = 2008
