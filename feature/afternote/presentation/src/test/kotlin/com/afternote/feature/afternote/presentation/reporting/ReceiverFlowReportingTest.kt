package com.afternote.feature.afternote.presentation.reporting

import com.afternote.feature.receiver.domain.error.ReceiverFailure
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * [shouldReportInReceiverFlow] 회귀 가드.
 *
 * 이 서버는 5xx 응답에도 `message` 를 싣는다(실측 #511). 문구 유무만으로 가르면 정작 잡으려던
 * 장애가 텔레메트리에서 통째로 빠지므로, 제외는 "4xx + 문구" 로만 성립해야 한다.
 */
class ReceiverFlowReportingTest {
    @Test
    fun `4xx 에 서버 문구가 실렸으면 사용자 오류라 제외한다`() {
        val rejection =
            ReceiverFailure.ServerRejection(
                status = 400,
                serverMessage = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.",
                serverCode = 1902,
                cause = CAUSE,
            )

        assertFalse(rejection.shouldReportInReceiverFlow())
    }

    @Test
    fun `5xx 는 서버 문구가 실려 있어도 기록 대상이다`() {
        val outage =
            ReceiverFailure.ServerRejection(
                status = 500,
                serverMessage = "서버 내부 오류: could not execute statement",
                serverCode = 1500,
                cause = CAUSE,
            )

        assertTrue(outage.shouldReportInReceiverFlow())
    }

    @Test
    fun `4xx 라도 서버 문구가 없으면 기록 대상이다`() {
        val silentRejection =
            ReceiverFailure.ServerRejection(
                status = 409,
                serverMessage = null,
                serverCode = 1700,
                cause = CAUSE,
            )

        assertTrue(silentRejection.shouldReportInReceiverFlow())
    }

    @Test
    fun `수신자 거절이 아닌 인프라 예외는 기록 대상이다`() {
        assertTrue(IOException("timeout").shouldReportInReceiverFlow())
    }
}

/**
 * ServerRejection 이 나르는 원인 예외 자리. 프로덕션에서는 `ApiException` 이 들어오지만, 도메인 계약이
 * 요구하는 것은 `Throwable` 뿐이라 이 테스트들은 core:network 를 끌어오지 않는다.
 */
private val CAUSE: Throwable = IOException("stub cause")
