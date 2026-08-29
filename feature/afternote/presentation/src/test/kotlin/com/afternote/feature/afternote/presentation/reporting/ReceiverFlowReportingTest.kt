package com.afternote.feature.afternote.presentation.reporting

import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * [shouldReportInReceiverFlow] 회귀 가드.
 *
 * status·서버 문구 판정은 Data 계층이 끝냈다. presentation 은 사용자 거절만 제외하고 예상 밖 서버
 * 실패와 그 밖의 예외는 기록해야 한다.
 */
class ReceiverFlowReportingTest {
    @Test
    fun `표시 사유가 있는 사용자 거절은 기록에서 제외한다`() {
        val rejection =
            ReceiverFailure.UserRejection(
                reason = ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND,
                cause = CAUSE,
            )

        assertFalse(rejection.shouldReportInReceiverFlow())
    }

    @Test
    fun `표시 사유 없는 사용자 거절도 기록에서 제외한다`() {
        val rejection = ReceiverFailure.UserRejection(reason = null, cause = CAUSE)

        assertFalse(rejection.shouldReportInReceiverFlow())
    }

    @Test
    fun `data 가 5xx 나 문구 없는 미등재 4xx 로 번역한 예상 밖 서버 실패는 기록 대상이다`() {
        val outage = ReceiverFailure.UnexpectedServerFailure(CAUSE)

        assertTrue(outage.shouldReportInReceiverFlow())
    }

    @Test
    fun `전달 조건 미충족은 예상된 사용자 거절이라 기록에서 제외한다`() {
        val notDeliverable = ReceiverFailure.DeliveryConditionNotMet(CAUSE)

        assertFalse(notDeliverable.shouldReportInReceiverFlow())
    }

    @Test
    fun `수신자 거절이 아닌 인프라 예외는 기록 대상이다`() {
        assertTrue(IOException("timeout").shouldReportInReceiverFlow())
    }
}

/** 프로덕션에서는 원인 자리에 `ApiException` 이 들어오지만, presentation 테스트는 network 를 알지 않는다. */
private val CAUSE: Throwable = IOException("stub cause")
