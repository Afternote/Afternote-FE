package com.afternote.feature.receiver.presentation.error

import com.afternote.feature.receiver.domain.error.ReceiverFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            ReceiverFailure.ServerRejection(
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
            ReceiverFailure.ServerRejection(
                status = 400,
                serverMessage = "인증번호는 UUID 형식이어야 합니다.",
                serverCode = 400,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), validationRejection.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `5xx 는 서버 문구가 실려 있어도 fallback 리소스로 폴백한다`() {
        val outage =
            ReceiverFailure.ServerRejection(
                status = 500,
                serverMessage = "서버 내부 오류: could not execute statement",
                serverCode = 1500,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), outage.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `allowlist 에 등재된 code 라도 5xx 봉투면 fallback 리소스로 폴백한다`() {
        val outageWithKnownCode =
            ReceiverFailure.ServerRejection(
                status = 500,
                serverMessage = "internal error while checking pending verification",
                serverCode = 2008,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), outageWithKnownCode.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `서버 문구 없는 4xx 는 등재 code 라도 fallback 리소스로 폴백한다`() {
        val silentRejection =
            ReceiverFailure.ServerRejection(
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

    /**
     * 등재 근거 — BE `ErrorCode.DELIVERY_CONDITION_NOT_MET(HttpStatus.FORBIDDEN, 2009,
     * "아직 전달 조건이 충족되지 않았습니다.")` (Afternote-BE `ErrorCode.java` 실코드). 문구 자체가
     * 사용자 안내라 그대로 노출한다.
     */
    @Test
    fun `전달 조건 미충족 2009 는 서버 문구를 그대로 노출한다`() {
        val notDeliverable =
            ReceiverFailure.ServerRejection(
                status = 403,
                serverMessage = "아직 전달 조건이 충족되지 않았습니다.",
                serverCode = DELIVERY_CONDITION_NOT_MET,
            )

        assertEquals(
            ErrorPayload.Text("아직 전달 조건이 충족되지 않았습니다."),
            notDeliverable.toErrorPayload(FALLBACK_RES),
        )
    }

    @Test
    fun `전달 조건 미충족만 재시도 불가로 갈린다`() {
        val notDeliverable =
            ReceiverFailure.ServerRejection(
                status = 403,
                serverMessage = "아직 전달 조건이 충족되지 않았습니다.",
                serverCode = DELIVERY_CONDITION_NOT_MET,
            )

        assertTrue(notDeliverable.isDeliveryConditionNotMet())
    }

    @Test
    fun `같은 403 이라도 다른 사유 code 는 재시도 경로에 남는다`() {
        val otherRejection =
            ReceiverFailure.ServerRejection(status = 403, serverMessage = "권한이 없습니다.", serverCode = 1903)

        assertFalse(otherRejection.isDeliveryConditionNotMet())
        assertFalse(IOException("offline").isDeliveryConditionNotMet())
    }

    private companion object {
        const val FALLBACK_RES = 1
    }
}
