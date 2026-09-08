package com.afternote.core.data.repoimpl

import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.network.model.ApiException
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class PushSettingFailureMapperTest {
    @Test
    fun `전송 실패는 네트워크 도메인 실패로 변환한다`() {
        val cause = IOException("offline")

        val failure =
            assertThrows(PushSettingFailure.NetworkUnavailable::class.java) {
                mapPushSettingFailure<Unit> { throw cause }
            }

        assertSame(cause, failure.cause)
    }

    @Test
    fun `API 응답 실패는 서버 도메인 실패로 변환한다`() {
        val cause =
            ApiException(
                status = 503,
                code = 1503,
                serverMessage = null,
                fallbackMessage = "server unavailable",
            )

        val failure =
            assertThrows(PushSettingFailure.ServerUnavailable::class.java) {
                mapPushSettingFailure<Unit> { throw cause }
            }

        assertSame(cause, failure.cause)
    }

    @Test
    fun `분류하지 않는 실패는 원본을 유지한다`() {
        val cause = IllegalStateException("unexpected")

        val failure =
            assertThrows(IllegalStateException::class.java) {
                mapPushSettingFailure<Unit> { throw cause }
            }

        assertSame(cause, failure)
    }
}
