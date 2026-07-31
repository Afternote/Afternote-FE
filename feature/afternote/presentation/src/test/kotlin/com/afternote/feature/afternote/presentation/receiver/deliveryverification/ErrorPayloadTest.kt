package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import com.afternote.feature.afternote.domain.error.ReceiverDeliveryVerificationException
import com.afternote.feature.afternote.domain.error.ReceiverEmailAuthException
import com.afternote.feature.afternote.domain.error.ReceiverMasterKeyException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * [toErrorPayload] 회귀 가드 — 서버 문구는 안내된 4xx 거절에서만 화면에 실린다는 규약.
 *
 * 이 서버는 5xx 봉투에도 내부 문구를 싣는다(500 body 에 SQL 원문 실측 #511). status 게이트가
 * 무너지면 수신자 열람 인증 3개 화면 스낵바에 그 원문이 그대로 노출된다(#651).
 */
class ErrorPayloadTest {
    @Test
    fun `4xx 에 서버 문구가 실렸으면 그대로 노출한다`() {
        val rejection =
            ReceiverEmailAuthException(
                status = 400,
                serverMessage = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.",
                serverCode = 1902,
            )

        assertEquals(
            ErrorPayload.Text("인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요."),
            rejection.toErrorPayload(FALLBACK_RES),
        )
    }

    @Test
    fun `5xx 는 서버 문구가 실려 있어도 fallback 리소스로 폴백한다`() {
        val outage =
            ReceiverDeliveryVerificationException(
                status = 500,
                serverMessage = "서버 내부 오류: could not execute statement",
                serverCode = 1500,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), outage.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `서버 문구 없는 4xx 는 fallback 리소스로 폴백한다`() {
        val silentRejection =
            ReceiverMasterKeyException(
                status = 404,
                serverMessage = null,
                serverCode = 1600,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), silentRejection.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `수신자 거절이 아닌 인프라 예외는 fallback 리소스로 폴백한다`() {
        assertEquals(ErrorPayload.Res(FALLBACK_RES), IOException("timeout").toErrorPayload(FALLBACK_RES))
    }

    private companion object {
        const val FALLBACK_RES = 1
    }
}
