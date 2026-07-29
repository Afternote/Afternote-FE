package com.afternote.feature.afternote.presentation.reporting

import com.afternote.feature.afternote.domain.error.ReceiverDeliveryVerificationException
import com.afternote.feature.afternote.domain.error.ReceiverEmailAuthException
import com.afternote.feature.afternote.domain.error.ReceiverMasterKeyException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * [isExplainedReceiverRejection] 회귀 가드.
 *
 * 이 서버는 5xx 응답에도 `message` 를 싣는다(실측 #511). 문구 유무만으로 가르면 정작 잡으려던
 * 장애가 텔레메트리에서 통째로 빠지므로, 제외는 "4xx + 문구" 로만 성립해야 한다.
 */
class ExplainedReceiverRejectionTest {
    @Test
    fun `4xx 에 서버 문구가 실렸으면 사용자 오류라 제외한다`() {
        val rejection =
            ReceiverEmailAuthException(
                status = 400,
                serverMessage = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.",
                serverCode = 1902,
            )

        assertTrue(rejection.isExplainedReceiverRejection())
    }

    @Test
    fun `5xx 는 서버 문구가 실려 있어도 기록 대상이다`() {
        val outage =
            ReceiverMasterKeyException(
                status = 500,
                serverMessage = "서버 내부 오류: could not execute statement",
                serverCode = 1500,
            )

        assertFalse(outage.isExplainedReceiverRejection())
    }

    @Test
    fun `4xx 라도 서버 문구가 없으면 기록 대상이다`() {
        val silentRejection =
            ReceiverDeliveryVerificationException(
                status = 409,
                serverMessage = null,
                serverCode = 1700,
            )

        assertFalse(silentRejection.isExplainedReceiverRejection())
    }

    @Test
    fun `수신자 거절이 아닌 인프라 예외는 기록 대상이다`() {
        assertFalse(IOException("timeout").isExplainedReceiverRejection())
    }
}
