package com.afternote.feature.afternote.presentation.receiver.senderdetail

import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderEntry
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SenderDetailRecordBoxMappingTest {
    @Test
    fun `열람 가능 - 인증 상태와 무관하게 승인 상태와 서버 신청 승인일을 표시`() {
        val entry =
            serverEntry(
                viewStatus = ReceivedRecordViewStatus.Viewable,
                verificationStatus = null,
                requestedAt = "2026-07-29T16:58:36",
                approvedAt = "2026-07-30T04:25:42",
            )

        val state = entry.toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.Approved, state.verification)
        assertEquals("2026.07.29.", state.requestedAt)
        assertEquals("2026.07.30.", state.approvedAt)
    }

    @Test
    fun `열람 대기 - 인증 상태가 불일치해도 재신청 대신 대기 상태 유지`() {
        val state =
            serverEntry(
                viewStatus = ReceivedRecordViewStatus.Pending,
                verificationStatus = DeliveryVerificationStatus.REJECTED,
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.Pending, state.verification)
        assertNull(state.approvedAt)
    }

    @Test
    fun `열람 신청 가능이고 최근 인증 거절 - 거절 상태 표시`() {
        val state =
            serverEntry(
                viewStatus = ReceivedRecordViewStatus.Requestable,
                verificationStatus = DeliveryVerificationStatus.REJECTED,
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.Rejected, state.verification)
    }

    @Test
    fun `열람 신청 가능인데 승인 값만 남은 불일치 - 열람 가능으로 오판하지 않음`() {
        val state =
            serverEntry(
                viewStatus = ReceivedRecordViewStatus.Requestable,
                verificationStatus = DeliveryVerificationStatus.APPROVED,
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.NotRequested, state.verification)
    }

    @Test
    fun `알 수 없는 열람 상태와 승인 값 - 열람 가능으로 오판하지 않음`() {
        val state =
            serverEntry(
                viewStatus = ReceivedRecordViewStatus.Unknown,
                verificationStatus = DeliveryVerificationStatus.APPROVED,
            ).toRecordBoxSuccessState()

        assertEquals(SenderVerificationState.NotRequested, state.verification)
    }
}

private fun serverEntry(
    viewStatus: ReceivedRecordViewStatus,
    verificationStatus: DeliveryVerificationStatus?,
    requestedAt: String? = null,
    approvedAt: String? = null,
): SenderEntry =
    SenderEntry(
        id = "record-box:18",
        name = "별칭",
        receiverId = 18L,
        authCode = "record-key",
        realSenderName = "김혜성",
        receiverName = "김지은",
        relation = "DAUGHTER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = viewStatus,
        verificationStatus = verificationStatus,
        requestedAt = requestedAt,
        approvedAt = approvedAt,
    )
