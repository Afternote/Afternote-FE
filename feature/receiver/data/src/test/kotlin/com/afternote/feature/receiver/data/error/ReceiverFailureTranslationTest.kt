package com.afternote.feature.receiver.data.error

import com.afternote.core.network.model.ApiException
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * HTTP·BE 오류 봉투를 수신자 도메인 어휘로 번역하는 경계의 회귀 가드(#1053).
 *
 * 서버 봉투 해석기를 이름으로 부르지 않고 공개 입구인 [toReceiverFailure] 로 들어간다 — 그
 * 해석기는 이 파일 밖에서 쓰이지 않으므로 `private` 다(#1678 가드). `ApiException` 을 넣으면
 * 입구가 곧장 그 해석기로 내려가므로 판정 범위는 그대로다.
 */
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

            val failure = original.toReceiverFailure()

            assertTrue("code=$code: $failure", failure is ReceiverFailure.UserRejection)
            failure as ReceiverFailure.UserRejection
            assertEquals("code=$code", expectedReason, failure.reason)
            assertSame("code=$code", original, failure.cause)
        }
    }

    @Test
    fun `미등재 4xx 는 문구가 있으면 표시 사유 없는 사용자 거절이다`() {
        val original = apiException(status = 422, code = 4999, serverMessage = "개발자용 검증 문구")

        val failure = original.toReceiverFailure()

        assertTrue(failure is ReceiverFailure.UserRejection)
        failure as ReceiverFailure.UserRejection
        assertNull(failure.reason)
        assertSame(original, failure.cause)
    }

    @Test
    fun `등재 5개 code 는 서버 문구가 없어도 확정 사유로 번역한다`() {
        val cases =
            listOf(
                Triple(404, 1900, ReceiverRejectionReason.INVALID_AUTH_CODE),
                Triple(404, 1901, ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND),
                Triple(400, 1902, ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND),
                Triple(400, 1903, ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_MISMATCH),
                Triple(409, 2008, ReceiverRejectionReason.VERIFICATION_ALREADY_SUBMITTED),
            )

        cases.forEach { (status, code, expectedReason) ->
            listOf(null, "  \n  ").forEach { serverMessage ->
                val original = apiException(status = status, code = code, serverMessage = serverMessage)

                val failure = original.toReceiverFailure()

                assertTrue(
                    "code=$code serverMessage=$serverMessage: $failure",
                    failure is ReceiverFailure.UserRejection,
                )
                failure as ReceiverFailure.UserRejection
                assertEquals("code=$code serverMessage=$serverMessage", expectedReason, failure.reason)
                assertSame("code=$code serverMessage=$serverMessage", original, failure.cause)
            }
        }
    }

    @Test
    fun `문구 없는 4xx 는 미등재 code 면 예상 밖 서버 실패다`() {
        listOf(null, "  \n  ").forEach { serverMessage ->
            val original = apiException(status = 422, code = 4999, serverMessage = serverMessage)

            val failure = original.toReceiverFailure()

            assertTrue("serverMessage=$serverMessage: $failure", failure is ReceiverFailure.UnexpectedServerFailure)
            assertSame(original, failure.cause)
        }
    }

    @Test
    fun `등재 code 여도 5xx 봉투면 예상 밖 서버 실패다`() {
        val original = apiException(status = 500, code = 2008, serverMessage = "내부 처리 오류")

        val failure = original.toReceiverFailure()

        assertTrue(failure is ReceiverFailure.UnexpectedServerFailure)
        assertSame(original, failure.cause)
    }

    @Test
    fun `전달 조건 미충족 2009 는 4xx 면 문구와 무관하게 전용 사유다`() {
        val original = apiException(status = 403, code = 2009, serverMessage = "아직 전달 조건이 충족되지 않았습니다.")

        val failure = original.toReceiverFailure()

        assertTrue(failure is ReceiverFailure.DeliveryConditionNotMet)
        assertSame(original, failure.cause)
    }

    @Test
    fun `2009 여도 5xx 봉투면 예상 밖 서버 실패다`() {
        val original = apiException(status = 500, code = 2009, serverMessage = "내부 처리 오류")

        val failure = original.toReceiverFailure()

        assertTrue(failure is ReceiverFailure.UnexpectedServerFailure)
        assertSame(original, failure.cause)
    }

    @Test
    fun `2009 는 문구 없는 4xx 여도 전달 조건 미충족이다`() {
        listOf(null, "  \n  ").forEach { serverMessage ->
            val original = apiException(status = 403, code = 2009, serverMessage = serverMessage)

            val failure = original.toReceiverFailure()

            assertTrue("serverMessage=$serverMessage: $failure", failure is ReceiverFailure.DeliveryConditionNotMet)
            assertSame(original, failure.cause)
        }
    }

    @Test
    fun `성공 결과는 값을 그대로 돌려준다`() {
        val result = Result.success("ok").mapReceiverFailure()

        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun `서버 거절 실패는 도메인 어휘로 옮긴다`() {
        val original = apiException(status = 404, code = 1901, serverMessage = "등록된 수신자 이메일이 아닙니다.")

        val failure = Result.failure<Unit>(original).mapReceiverFailure().exceptionOrNull()

        assertTrue("$failure", failure is ReceiverFailure.UserRejection)
        assertEquals(ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND, (failure as ReceiverFailure.UserRejection).reason)
        assertSame(original, failure.cause)
    }

    @Test
    fun `전송 계층 실패는 연결 불가로 옮긴다`() {
        val original = IOException("Unable to resolve host")

        val failure = Result.failure<Unit>(original).mapReceiverFailure().exceptionOrNull()

        assertTrue("$failure", failure is ReceiverFailure.NetworkUnavailable)
        assertSame(original, failure?.cause)
    }

    /** 도메인 어휘가 없는 실패까지 감싸면 매핑 실패 같은 진단 신호가 소비처에서 사라진다. */
    @Test
    fun `분류 대상이 아닌 실패는 원본 인스턴스 그대로 나간다`() {
        val original = IllegalStateException("boom")

        val failure = Result.failure<Unit>(original).mapReceiverFailure().exceptionOrNull()

        assertSame(original, failure)
    }

    /** 위임으로 두 번 지나는 경로(문서 업로드 → presigned URL 발급) 가 실패를 겹겹이 감싸지 않는지. */
    @Test
    fun `이미 번역된 실패는 다시 감싸지 않는다`() {
        val original = ReceiverFailure.DeliveryConditionNotMet(apiException(status = 403, code = 2009, serverMessage = null))

        val failure = Result.failure<Unit>(original).mapReceiverFailure().exceptionOrNull()

        assertSame(original, failure)
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
