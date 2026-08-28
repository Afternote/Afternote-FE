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
 * 서버 응답의 status·code·message 를 도메인 어휘로 번역한다.
 *
 * 노출과 리포팅 판정의 폭은 의도적으로 다르다. `4xx + 비어 있지 않은 서버 문구`면 서버가 예상하고
 * 처리한 [ReceiverFailure.UserRejection] 이므로 리포팅에서 제외한다. 그중 화면에 별도 안내가 필요한
 * 5개 code 만 [ReceiverRejectionReason] 으로 채우고, 미등재 code 는 `reason = null` 로 두어 서버 원문을
 * 노출하지 않는다. 5xx 또는 문구 없는 4xx 는 [ReceiverFailure.UnexpectedServerFailure] 로 기록한다.
 * [ReceiverFailure.DeliveryConditionNotMet]도 같은 사용자 거절 게이트를 통과한 code 2009만 승격한다.
 *
 * **BE `ErrorCode` 번호를 아는 것은 이 계층까지다.** 도메인 실패는 status·code·message 어느 것도
 * 운반하지 않으며, presentation 은 번역된 타입과 사유만 소비한다.
 */
internal fun ApiException.toReceiverServerFailure(): ReceiverFailure =
    when {
        status in CLIENT_ERROR_STATUS_RANGE && !serverMessage.isNullOrBlank() -> {
            when (code) {
                DELIVERY_CONDITION_NOT_MET -> {
                    ReceiverFailure.DeliveryConditionNotMet(this)
                }

                else -> {
                    ReceiverFailure.UserRejection(
                        reason = code.toReceiverRejectionReasonOrNull(),
                        cause = this,
                    )
                }
            }
        }

        else -> {
            ReceiverFailure.UnexpectedServerFailure(this)
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
