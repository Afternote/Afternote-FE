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
                cause = CAUSE,
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
                cause = CAUSE,
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
                cause = CAUSE,
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
                cause = CAUSE,
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
                cause = CAUSE,
            )

        assertEquals(ErrorPayload.Res(FALLBACK_RES), silentRejection.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `수신자 거절이 아닌 인프라 예외는 fallback 리소스로 폴백한다`() {
        assertEquals(ErrorPayload.Res(FALLBACK_RES), IOException("timeout").toErrorPayload(FALLBACK_RES))
    }

    /** 사유가 타입으로 특정된 거절은 서버 문구를 싣지 않는다 — 표시 문구는 호출처 리소스가 갖는다. */
    @Test
    fun `전달 조건 미충족은 서버 문구 대신 호출처 리소스로 내려앉는다`() {
        val notDeliverable = ReceiverFailure.DeliveryConditionNotMet(CAUSE)

        assertEquals(ErrorPayload.Res(FALLBACK_RES), notDeliverable.toErrorPayload(FALLBACK_RES))
    }

    @Test
    fun `전달 조건 미충족만 재시도 불가로 갈린다`() {
        val notDeliverable = ReceiverFailure.DeliveryConditionNotMet(CAUSE)

        assertTrue(notDeliverable.isDeliveryConditionNotMet())
    }

    @Test
    fun `사유를 가르지 않은 403 은 재시도 경로에 남는다`() {
        val otherRejection =
            ReceiverFailure.ServerRejection(status = 403, serverMessage = "권한이 없습니다.", serverCode = 1903, cause = CAUSE)

        assertFalse(otherRejection.isDeliveryConditionNotMet())
        assertFalse(IOException("offline").isDeliveryConditionNotMet())
    }

    @Test
    fun `연결 실패는 노출할 서버 문구가 없어 fallback 리소스로 폴백한다`() {
        val offline = ReceiverFailure.NetworkUnavailable(IOException("Unable to resolve host"))

        assertEquals(ErrorPayload.Res(FALLBACK_RES), offline.toErrorPayload(FALLBACK_RES))
        assertFalse(offline.isDeliveryConditionNotMet())
    }

    private companion object {
        const val FALLBACK_RES = 1
    }
}

/**
 * ServerRejection 이 나르는 원인 예외 자리. 프로덕션에서는 `ApiException` 이 들어오지만, 도메인 계약이
 * 요구하는 것은 `Throwable` 뿐이라 이 테스트들은 core:network 를 끌어오지 않는다.
 */
private val CAUSE: Throwable = IOException("stub cause")
