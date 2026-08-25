package com.afternote.feature.setting.presentation.social

import com.afternote.core.domain.error.CoreAuthFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class KakaoAuthResultTest {
    @Test
    fun `success contains access token`() {
        val result = Result.success("access-token").toKakaoAuthResult()

        assertEquals(KakaoAuthResult.Success("access-token"), result)
    }

    @Test
    fun `user cancellation is cancelled`() {
        val result = Result.failure<String>(CoreAuthFailure.UserCancelledAuth()).toKakaoAuthResult()

        assertEquals(KakaoAuthResult.Cancelled, result)
    }

    @Test
    fun `authentication failure is failure`() {
        val result = Result.failure<String>(IllegalStateException("authentication failed")).toKakaoAuthResult()

        assertEquals(KakaoAuthResult.Failure, result)
    }

    @Test
    fun `blank access token is failure`() {
        val result = Result.success(" ").toKakaoAuthResult()

        assertEquals(KakaoAuthResult.Failure, result)
    }
}
