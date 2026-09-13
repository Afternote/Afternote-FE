package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup
import com.afternote.feature.receiver.presentation.senderdetail.SenderDetailReducerEvent
import com.afternote.feature.receiver.presentation.senderdetail.SenderDetailUiState
import com.afternote.feature.receiver.presentation.senderdetail.SenderVerificationState
import com.afternote.feature.receiver.presentation.senderdetail.reduceSenderDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** 코루틴과 저장소 없이 재첨부·백그라운드 갱신·신호 소비가 보존해야 하는 상태를 검증한다. */
class ReceiverReducerTest {
    @Test
    fun `재첨부 실패 복원은 다른 슬롯의 동시 업로드 결과를 유지한다`() {
        val original = DocumentSlotState(displayName = "원본.pdf", fileUrl = "original-url")
        val initial = DocumentUploadUiState(deathCertificate = original)
        val uploading = reduceDocumentUpload(initial, DocumentUploadReducerEvent.UploadStarted(DocumentSlot.DeathCertificate, "교체.pdf"))
        val otherCompleted =
            reduceDocumentUpload(uploading, DocumentUploadReducerEvent.Uploaded(DocumentSlot.FamilyRelationCertificate, "family-url"))

        val restored =
            reduceDocumentUpload(otherCompleted, DocumentUploadReducerEvent.UploadRestored(DocumentSlot.DeathCertificate, original))

        assertEquals(original, restored.deathCertificate)
        assertEquals("family-url", restored.familyRelationCertificate.fileUrl)
        assertTrue(restored.canSubmit)
    }

    @Test
    fun `한 슬롯에 URL이 있어도 다른 슬롯 업로드 중에는 제출할 수 없다`() {
        val state = DocumentUploadUiState(deathCertificate = DocumentSlotState(fileUrl = "original-url"))

        val uploading =
            reduceDocumentUpload(state, DocumentUploadReducerEvent.UploadStarted(DocumentSlot.FamilyRelationCertificate, "가족.pdf"))

        assertFalse(uploading.canSubmit)
        assertEquals("original-url", uploading.deathCertificate.fileUrl)
    }

    @Test
    fun `제출 신호 소비는 업로드 결과와 실패 팝업을 보존한다`() {
        val state =
            DocumentUploadUiState(
                deathCertificate = DocumentSlotState(fileUrl = "original-url"),
                isSubmitted = true,
                errorPopup = ReceiverErrorPopup.NETWORK,
            )

        val consumed = reduceDocumentUpload(state, DocumentUploadReducerEvent.SubmittedConsumed)

        assertFalse(consumed.isSubmitted)
        assertEquals(state.deathCertificate, consumed.deathCertificate)
        assertEquals(state.errorPopup, consumed.errorPopup)
    }

    @Test
    fun `이메일 입력은 공백을 보존하고 trim한 형식을 판정하며 이전 안내를 해제한다`() {
        val initial = IdentityVerificationUiState(errorMessage = UiText.Resource(R.string.receiver_verify_code_send_failed))

        val changed = reduceIdentityVerification(initial, IdentityVerificationReducerEvent.EmailChanged(" receiver@example.test "))

        assertEquals(" receiver@example.test ", changed.email)
        assertTrue(changed.isEmailFormatValid)
        assertNull(changed.errorMessage)
    }

    @Test
    fun `인증 신호 소비는 입력과 인증번호 발송 상태를 유지한다`() {
        val state =
            IdentityVerificationUiState(email = "receiver@example.test", code = "123456", isVerificationSent = true, isVerified = true)

        val consumed = reduceIdentityVerification(state, IdentityVerificationReducerEvent.VerifiedConsumed)

        assertFalse(consumed.isVerified)
        assertEquals(state.email, consumed.email)
        assertEquals(state.code, consumed.code)
        assertTrue(consumed.isVerificationSent)
    }

    @Test
    fun `상세 새로고침 완료는 미소비 홈 이동을 잃지 않는다`() {
        val current = success(shouldOpenReceiverHome = true)
        val loaded = success(shouldOpenReceiverHome = false).copy(verification = SenderVerificationState.Approved)

        val refreshed =
            reduceSenderDetail(
                current,
                SenderDetailReducerEvent.Loaded(loaded, keepsStateOnFailure = true),
            ) as SenderDetailUiState.Success

        assertTrue(refreshed.shouldOpenReceiverHome)
        assertEquals(SenderVerificationState.Approved, refreshed.verification)
    }

    @Test
    fun `복귀 갱신 실패는 보고 있던 상세를 보존하고 첫 로드 실패는 표시한다`() {
        val current = success(shouldOpenReceiverHome = true)
        val failure = SenderDetailUiState.StatusLoadFailed("발신자")

        assertSame(current, reduceSenderDetail(current, SenderDetailReducerEvent.Loaded(failure, keepsStateOnFailure = true)))
        assertEquals(
            failure,
            reduceSenderDetail(SenderDetailUiState.Loading, SenderDetailReducerEvent.Loaded(failure, keepsStateOnFailure = false)),
        )
    }

    private fun success(shouldOpenReceiverHome: Boolean) =
        SenderDetailUiState.Success(
            displayName = "발신자",
            verification = SenderVerificationState.Pending,
            requestedAt = "2026.09.01.",
            approvedAt = null,
            shouldOpenReceiverHome = shouldOpenReceiverHome,
        )
}
