package com.afternote.feature.receiver.presentation.error

import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import com.afternote.feature.receiver.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * [toReceiverErrorUiText] 회귀 가드 — Data 계층이 등재한 사용자 거절 사유만 로컬 리소스로 표시한다.
 *
 * HTTP status·BE code·서버 message 판정은 `ReceiverFailureTranslationTest` 가 맡는다. 여기서는 도메인
 * 사유와 리소스의 연결 및 미등재 거절·장애의 폴백만 검증한다.
 */
class ReceiverErrorUiTextTest {
    @Test
    fun `등재된 5개 사용자 거절 사유는 각각 로컬 리소스로 표시한다`() {
        val cases =
            listOf(
                ReceiverRejectionReason.INVALID_AUTH_CODE to R.string.receiver_error_invalid_auth_code,
                ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND to R.string.receiver_error_email_not_found,
                ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND to
                    R.string.receiver_error_email_auth_code_not_found,
                ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_MISMATCH to
                    R.string.receiver_error_email_auth_code_mismatch,
                ReceiverRejectionReason.VERIFICATION_ALREADY_SUBMITTED to
                    R.string.receiver_error_verification_already_submitted,
            )

        cases.forEach { (reason, expectedRes) ->
            val rejection = ReceiverFailure.UserRejection(reason = reason, cause = CAUSE)

            assertEquals(UiText.Resource(expectedRes), rejection.toReceiverErrorUiText(FALLBACK_RES))
        }
    }

    @Test
    fun `표시 사유 없는 사용자 거절은 fallback 리소스로 폴백한다`() {
        val unclassifiedRejection = ReceiverFailure.UserRejection(reason = null, cause = CAUSE)

        assertEquals(fallbackUiText, unclassifiedRejection.toReceiverErrorUiText(FALLBACK_RES))
    }

    @Test
    fun `예상 밖 서버 실패는 fallback 리소스로 폴백한다`() {
        val outage = ReceiverFailure.UnexpectedServerFailure(CAUSE)

        assertEquals(fallbackUiText, outage.toReceiverErrorUiText(FALLBACK_RES))
    }

    @Test
    fun `수신자 거절이 아닌 인프라 예외는 fallback 리소스로 폴백한다`() {
        assertEquals(fallbackUiText, IOException("timeout").toReceiverErrorUiText(FALLBACK_RES))
    }

    /** 사유가 타입으로 특정된 거절은 서버 문구를 싣지 않는다 — 표시 문구는 호출처 리소스가 갖는다. */
    @Test
    fun `전달 조건 미충족은 서버 문구 대신 호출처 리소스로 내려앉는다`() {
        val notDeliverable = ReceiverFailure.DeliveryConditionNotMet(CAUSE)

        assertEquals(fallbackUiText, notDeliverable.toReceiverErrorUiText(FALLBACK_RES))
    }

    @Test
    fun `연결 실패는 노출할 서버 문구가 없어 fallback 리소스로 폴백한다`() {
        val offline = ReceiverFailure.NetworkUnavailable(IOException("Unable to resolve host"))

        assertEquals(fallbackUiText, offline.toReceiverErrorUiText(FALLBACK_RES))
    }

    private companion object {
        const val FALLBACK_RES = 1
        val fallbackUiText = UiText.Resource(FALLBACK_RES)
    }
}

/** 프로덕션에서는 원인 자리에 `ApiException` 이 들어오지만, presentation 테스트는 network 를 알지 않는다. */
private val CAUSE: Throwable = IOException("stub cause")
