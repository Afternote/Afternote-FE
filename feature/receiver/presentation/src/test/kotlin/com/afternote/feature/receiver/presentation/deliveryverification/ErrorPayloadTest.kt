package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.feature.receiver.domain.error.ReceiverDeliveryVerificationException
import com.afternote.feature.receiver.domain.error.ReceiverEmailAuthException
import com.afternote.feature.receiver.domain.error.ReceiverMasterKeyException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * [toErrorPayload] 회귀 가드 — 서버 문구는 allowlist 에 등재된 4xx 거절에서만 화면에 실린다는 규약.
 *
 * 이 서버는 5xx 봉투에도 내부 문구를 싣고(500 body 에 SQL 원문 실측 #511), 4xx 라도 `@Valid` 검증
 * 실패는 개발자용 형식 문구를 리터럴 code=400 으로 싣는다("인증번호는 UUID 형식이어야 합니다."
 * #600 실측). 게이트가 무너지면 수신자 열람 인증 3개 화면 스낵바에 그 원문들이 그대로 노출된다.
 */
class ErrorPayloadTest {
    @Test
    fun `allowlist 에 등재된 4xx 사유 code 는 서버 문구를 그대로 노출한다`() {
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
    fun `allowlist 미등재 4xx 는 서버 문구가 있어도 fallback 리소스로 폴백한다`() {
        val validationRejection =
            ReceiverMasterKeyException(
                status = 400,
                serverMessage = "인증번호는 UUID 형식이어야 합니다.",
                serverCode = 400,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), validationRejection.toErrorPayload(FALLBACK_RES))
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
    fun `allowlist 에 등재된 code 라도 5xx 봉투면 fallback 리소스로 폴백한다`() {
        val outageWithKnownCode =
            ReceiverDeliveryVerificationException(
                status = 500,
                serverMessage = "internal error while checking pending verification",
                serverCode = 2008,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), outageWithKnownCode.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `서버 문구 없는 4xx 는 등재 code 라도 fallback 리소스로 폴백한다`() {
        val silentRejection =
            ReceiverMasterKeyException(
                status = 404,
                serverMessage = null,
                serverCode = 1900,
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
