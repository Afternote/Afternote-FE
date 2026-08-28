package com.afternote.feature.receiver.data.error

import com.afternote.core.network.model.ApiException
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** HTTP·BE 오류 봉투를 수신자 도메인 어휘로 번역하는 경계의 회귀 가드(#1053). */
class ReceiverFailureTranslationTest {
    @Test
    fun `표시 허용 5개 code 는 각각 도메인 사유로 번역한다`() {
        val cases =
            listOf(
                Triple(404, 1900, ReceiverRejectionReason.INVALID_AUTH_CODE),
                Triple(404, 1901, ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND),
                Triple(400, 1902, ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND),
                Triple(400, 1903, ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_MISMATCH),
                Triple(409, 2008, ReceiverRejectionReason.VERIFICATION_ALREADY_SUBMITTED),
            )

        cases.forEach { (status, code, expectedReason) ->
            val original = apiException(status = status, code = code, serverMessage = "사용자 안내")

            val failure = original.toReceiverServerFailure()

            assertTrue("code=$code: $failure", failure is ReceiverFailure.UserRejection)
            failure as ReceiverFailure.UserRejection
            assertEquals("code=$code", expectedReason, failure.reason)
            assertSame("code=$code", original, failure.cause)
        }
    }

    @Test
    fun `미등재 4xx 는 문구가 있으면 표시 사유 없는 사용자 거절이다`() {
        val original = apiException(status = 422, code = 4999, serverMessage = "개발자용 검증 문구")

        val failure = original.toReceiverServerFailure()

        assertTrue(failure is ReceiverFailure.UserRejection)
        failure as ReceiverFailure.UserRejection
        assertNull(failure.reason)
        assertSame(original, failure.cause)
    }

    @Test
    fun `문구 없는 4xx 는 등재 code 여도 예상 밖 서버 실패다`() {
        listOf(null, "  \n  ").forEach { serverMessage ->
            val original = apiException(status = 404, code = 1900, serverMessage = serverMessage)

            val failure = original.toReceiverServerFailure()

            assertTrue("serverMessage=$serverMessage: $failure", failure is ReceiverFailure.UnexpectedServerFailure)
            assertSame(original, failure.cause)
        }
    }

    @Test
    fun `등재 code 여도 5xx 봉투면 예상 밖 서버 실패다`() {
        val original = apiException(status = 500, code = 2008, serverMessage = "내부 처리 오류")

        val failure = original.toReceiverServerFailure()

        assertTrue(failure is ReceiverFailure.UnexpectedServerFailure)
        assertSame(original, failure.cause)
    }

    @Test
    fun `전달 조건 미충족 2009 는 4xx 와 문구가 함께 있을 때만 전용 사유다`() {
        val original = apiException(status = 403, code = 2009, serverMessage = "아직 전달 조건이 충족되지 않았습니다.")

        val failure = original.toReceiverServerFailure()

        assertTrue(failure is ReceiverFailure.DeliveryConditionNotMet)
        assertSame(original, failure.cause)
    }

    @Test
    fun `2009 여도 5xx 봉투면 예상 밖 서버 실패다`() {
        val original = apiException(status = 500, code = 2009, serverMessage = "내부 처리 오류")

        val failure = original.toReceiverServerFailure()

        assertTrue(failure is ReceiverFailure.UnexpectedServerFailure)
        assertSame(original, failure.cause)
    }

    @Test
    fun `2009 여도 문구 없는 4xx 면 예상 밖 서버 실패다`() {
        listOf(null, "  \n  ").forEach { serverMessage ->
            val original = apiException(status = 403, code = 2009, serverMessage = serverMessage)

            val failure = original.toReceiverServerFailure()

            assertTrue("serverMessage=$serverMessage: $failure", failure is ReceiverFailure.UnexpectedServerFailure)
            assertSame(original, failure.cause)
        }
    }

    private fun apiException(
        status: Int,
        code: Int,
        serverMessage: String?,
    ): ApiException =
        ApiException(
            status = status,
            code = code,
            serverMessage = serverMessage,
            fallbackMessage = "receiver request failed",
        )
}
