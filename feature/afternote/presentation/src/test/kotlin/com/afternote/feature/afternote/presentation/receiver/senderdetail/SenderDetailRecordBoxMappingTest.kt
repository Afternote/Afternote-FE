package com.afternote.feature.afternote.presentation.receiver.senderdetail

import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.afternote.feature.afternote.presentation.receiver.recordsbox.ReceivedRecordItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SenderDetailRecordBoxMappingTest {
    @Test
    fun `열람 가능 - 승인 상태와 서버 신청 승인일을 표시`() {
        val entry =
            recordBoxEntry(
                viewStatus = ReceivedRecordViewStatus.Viewable,
                verification =
                    ReceivedRecordVerification.Approved(
                        requestedAt = "2026-07-29T16:58:36",
                        approvedAt = "2026-07-30T04:25:42",
                    ),
            )

        val state = entry.toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.Approved, state.verification)
        assertEquals("2026.07.29.", state.requestedAt)
        assertEquals("2026.07.30.", state.approvedAt)
    }

    @Test
    fun `열람 대기 - 인증 상태가 불일치해도 재신청 대신 대기 상태 유지`() {
        val state =
            recordBoxEntry(
                viewStatus = ReceivedRecordViewStatus.Pending,
                verification = ReceivedRecordVerification.Rejected("2026-07-29T16:58:36"),
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.Pending, state.verification)
        assertNull(state.approvedAt)
    }

    @Test
    fun `열람 신청 가능이고 최근 인증 거절 - 거절 상태 표시`() {
        val state =
            recordBoxEntry(
                viewStatus = ReceivedRecordViewStatus.Requestable,
                verification = ReceivedRecordVerification.Rejected("2026-07-29T16:58:36"),
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.Rejected, state.verification)
    }

    @Test
    fun `열람 신청 가능인데 승인 값만 남은 불일치 - 열람 가능으로 오판하지 않음`() {
        val state =
            recordBoxEntry(
                viewStatus = ReceivedRecordViewStatus.Requestable,
                verification =
                    ReceivedRecordVerification.Approved(
                        requestedAt = "2026-07-29T16:58:36",
                        approvedAt = "2026-07-30T04:25:42",
                    ),
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.NotRequested, state.verification)
    }

    @Test
    fun `알 수 없는 열람 상태와 승인 값 - 열람 가능으로 오판하지 않음`() {
        val state =
            recordBoxEntry(
                viewStatus = ReceivedRecordViewStatus.Unknown,
                verification =
                    ReceivedRecordVerification.Approved(
                        requestedAt = "2026-07-29T16:58:36",
                        approvedAt = "2026-07-30T04:25:42",
                    ),
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.NotRequested, state.verification)
    }

    @Test
    fun `미지원 인증 상태 - 열람 가능으로 오판하지 않음`() {
        val state =
            recordBoxEntry(
                viewStatus = ReceivedRecordViewStatus.Requestable,
                verification = ReceivedRecordVerification.Unknown,
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.NotRequested, state.verification)
        assertNull(state.requestedAt)
        assertNull(state.approvedAt)
    }
}

private fun recordBoxEntry(
    viewStatus: ReceivedRecordViewStatus,
    verification: ReceivedRecordVerification,
): ReceivedRecordItem =
    ReceivedRecordItem(
        recordBoxId = 18L,
        accessCode = "record-key",
        senderName = "김혜성",
        receiverName = "김지은",
        relation = "DAUGHTER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = viewStatus,
        verification = verification,
    )
