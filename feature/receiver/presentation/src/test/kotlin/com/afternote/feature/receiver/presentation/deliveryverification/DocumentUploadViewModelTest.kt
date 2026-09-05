package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.presentation.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [DocumentUploadViewModel] 교차 슬롯 상태 전이 가드 (#711).
 *
 * 계약 — 어느 슬롯이든 업로드 진행 중이면 [DocumentUploadUiState.canSubmit] 이 잠기고
 * [DocumentUploadViewModel.submit] 도 진행 안내로 거절한다. 이미 성공한 URL 만 실려
 * 진행 중 파일이 신청에서 조용히 빠지는 것 방지. 진행 중 업로드가 완료·실패로 끝나면
 * 잠금이 풀리고, 한 장만으로도 제출 가능한 최소 1개 정책(#380)은 유지된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentUploadViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 관심사는 상태 전이뿐이라 계측은 버린다. */
    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private fun viewModel(
        uploads: FakeReceiverDeliveryDocumentUploadRepository,
        auth: FakeReceiverAuthRepository = submittingAuthRepository(),
    ): DocumentUploadViewModel =
        DocumentUploadViewModel(
            uploadRepository = uploads,
            receiverAuthRepository = auth,
            errorReporter = NoopErrorReporter,
        )

    private fun DocumentUploadViewModel.startUpload(slot: DocumentSlot) {
        uploadDocument(slot = slot, bytes = byteArrayOf(1), extension = "jpg", displayName = "서류.jpg")
    }

    @Test
    fun `한쪽 완료 후 다른 쪽 업로드 중 - canSubmit 잠기고 완료되면 풀림`() {
        val (uploads, pendingUploads) = gatedUploadRepository()
        val viewModel = viewModel(uploads)

        viewModel.startUpload(DocumentSlot.DeathCertificate)
        pendingUploads.completeOldest(Result.success("url-death"))
        assertTrue(viewModel.uiState.value.canSubmit)

        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)
        assertTrue(viewModel.uiState.value.familyRelationCertificate.isUploading)
        assertFalse(viewModel.uiState.value.canSubmit)

        pendingUploads.completeOldest(Result.success("url-family"))
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `업로드 진행 중 submit - 진행 안내로 거절되고 신청 API 미호출`() {
        val (uploads, pendingUploads) = gatedUploadRepository()
        val auth = submittingAuthRepository()
        val viewModel = viewModel(uploads, auth)
        viewModel.startUpload(DocumentSlot.DeathCertificate)
        pendingUploads.completeOldest(Result.success("url-death"))
        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(R.string.receiver_verify_document_upload_in_progress), state.errorMessage)
        assertFalse(state.isSubmitting)
        assertTrue(auth.deliverySubmissions.isEmpty())
    }

    @Test
    fun `업로드 실패로 끝난 슬롯 - 잠금이 풀려 기존 첨부만으로 제출 가능 (#380 정책 유지)`() {
        val (uploads, pendingUploads) = gatedUploadRepository()
        val viewModel = viewModel(uploads)
        viewModel.startUpload(DocumentSlot.DeathCertificate)
        pendingUploads.completeOldest(Result.success("url-death"))

        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)
        pendingUploads.completeOldest(Result.failure(Exception("S3 PUT 실패")))

        val state = viewModel.uiState.value
        assertFalse(state.familyRelationCertificate.isUploading)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `모든 업로드 완료 후 submit - 두 URL 이 모두 페이로드에 실림`() {
        val (uploads, pendingUploads) = gatedUploadRepository()
        val auth = submittingAuthRepository()
        val viewModel = viewModel(uploads, auth)
        viewModel.startUpload(DocumentSlot.DeathCertificate)
        pendingUploads.completeOldest(Result.success("url-death"))
        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)
        pendingUploads.completeOldest(Result.success("url-family"))

        viewModel.submit()

        assertEquals(listOf("url-death" to "url-family"), auth.deliverySubmissions)
        assertTrue(viewModel.uiState.value.isSubmitted)
    }
}

private typealias PendingUploads = ArrayDeque<CompletableDeferred<Result<String>>>

private fun gatedUploadRepository(): Pair<FakeReceiverDeliveryDocumentUploadRepository, PendingUploads> {
    val pendingUploads = PendingUploads()
    val repository =
        FakeReceiverDeliveryDocumentUploadRepository.strict().apply {
            onUpload = { _, _ ->
                val deferred = CompletableDeferred<Result<String>>()
                pendingUploads.addLast(deferred)
                deferred.await()
            }
        }
    return repository to pendingUploads
}

private fun PendingUploads.completeOldest(result: Result<String>) {
    removeFirst().complete(result)
}

private fun submittingAuthRepository(): FakeReceiverAuthRepository =
    FakeReceiverAuthRepository.strict().apply {
        onSubmitDeliveryVerification = { deathCertificateUrl, familyRelationCertificateUrl ->
            Result.success(
                DeliveryVerification(
                    id = 1L,
                    status = DeliveryVerificationStatus.PENDING,
                    deathCertificateUrl = deathCertificateUrl,
                    familyRelationCertificateUrl = familyRelationCertificateUrl,
                    adminNote = null,
                    createdAt = null,
                ),
            )
        }
    }
