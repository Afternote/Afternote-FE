package com.afternote.core.data.repoimpl.invitation

import com.afternote.core.domain.error.ReceiverInvitationFailure
import com.afternote.core.network.model.ApiException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/** BE 초대 실패 code → 도메인 어휘 번역 (#944). */
class ReceiverInvitationFailureTranslationTest {
    @Test
    fun `등재 code 는 code 만으로 사유가 확정된다`() {
        assertTrue(api(401, 1000).translate() is ReceiverInvitationFailure.Unauthenticated)
        assertTrue(api(404, 1905).translate() is ReceiverInvitationFailure.NotFound)
        assertTrue(api(410, 1906).translate() is ReceiverInvitationFailure.Expired)
        assertTrue(api(409, 1907).translate() is ReceiverInvitationFailure.AcceptedByOther)
        assertTrue(api(400, 1908).translate() is ReceiverInvitationFailure.SelfAccept)
        assertTrue(api(409, 1909).translate() is ReceiverInvitationFailure.AlreadyRegistered)
    }

    @Test
    fun `5xx 는 등재 code 여도 재시도 가능한 장애다`() {
        val failure = api(500, 1905).translate()

        assertTrue(failure is ReceiverInvitationFailure.Other)
        assertTrue((failure as ReceiverInvitationFailure.Other).isRetryable)
    }

    @Test
    fun `전송 실패는 재시도 가능하다`() {
        val failure = UnknownHostException("no host").translate()

        assertTrue(failure is ReceiverInvitationFailure.Other)
        assertTrue((failure as ReceiverInvitationFailure.Other).isRetryable)
        assertTrue(failure.cause is IOException)
    }

    @Test
    fun `미등재 4xx 는 재시도 불가로 남긴다`() {
        val failure = api(400, 1400).translate()

        assertTrue(failure is ReceiverInvitationFailure.Other)
        assertFalse((failure as ReceiverInvitationFailure.Other).isRetryable)
    }

    @Test
    fun `원인 예외는 cause 로 보존된다`() {
        val apiError = api(404, 1905)

        assertSame(apiError, apiError.translate().cause)
    }

    private fun Throwable.translate(): ReceiverInvitationFailure =
        Result.failure<Unit>(this).mapReceiverInvitationFailure().exceptionOrNull() as ReceiverInvitationFailure

    private fun api(
        status: Int,
        code: Int,
    ) = ApiException(status = status, code = code, serverMessage = "server", fallbackMessage = "fallback")
}
