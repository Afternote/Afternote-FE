package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
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
        uploads: FakeUploadRepository,
        auth: FakeReceiverAuthRepository = FakeReceiverAuthRepository(),
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
        val uploads = FakeUploadRepository()
        val viewModel = viewModel(uploads)

        viewModel.startUpload(DocumentSlot.DeathCertificate)
        uploads.completeOldest(Result.success("url-death"))
        assertTrue(viewModel.uiState.value.canSubmit)

        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)
        assertTrue(viewModel.uiState.value.familyRelationCertificate.isUploading)
        assertFalse(viewModel.uiState.value.canSubmit)

        uploads.completeOldest(Result.success("url-family"))
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `업로드 진행 중 submit - 진행 안내로 거절되고 신청 API 미호출`() {
        val uploads = FakeUploadRepository()
        val auth = FakeReceiverAuthRepository()
        val viewModel = viewModel(uploads, auth)
        viewModel.startUpload(DocumentSlot.DeathCertificate)
        uploads.completeOldest(Result.success("url-death"))
        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)

        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(UiText.Resource(R.string.receiver_verify_document_upload_in_progress), state.error)
        assertFalse(state.isSubmitting)
        assertTrue(auth.submittedPayloads.isEmpty())
    }

    @Test
    fun `업로드 실패로 끝난 슬롯 - 잠금이 풀려 기존 첨부만으로 제출 가능 (#380 정책 유지)`() {
        val uploads = FakeUploadRepository()
        val viewModel = viewModel(uploads)
        viewModel.startUpload(DocumentSlot.DeathCertificate)
        uploads.completeOldest(Result.success("url-death"))

        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)
        uploads.completeOldest(Result.failure(Exception("S3 PUT 실패")))

        val state = viewModel.uiState.value
        assertFalse(state.familyRelationCertificate.isUploading)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `모든 업로드 완료 후 submit - 두 URL 이 모두 페이로드에 실림`() {
        val uploads = FakeUploadRepository()
        val auth = FakeReceiverAuthRepository()
        val viewModel = viewModel(uploads, auth)
        viewModel.startUpload(DocumentSlot.DeathCertificate)
        uploads.completeOldest(Result.success("url-death"))
        viewModel.startUpload(DocumentSlot.FamilyRelationCertificate)
        uploads.completeOldest(Result.success("url-family"))

        viewModel.submit()

        assertEquals(listOf("url-death" to "url-family"), auth.submittedPayloads)
        assertTrue(viewModel.uiState.value.isSubmitted)
    }
}

/**
 * [ReceiverDeliveryDocumentUploadRepository] 가짜 — [upload] 를 호출 순서대로 [CompletableDeferred]
 * 에 매달아 두어, 테스트가 "업로드 진행 중" 상태를 원하는 시점까지 유지·해소할 수 있게 한다.
 */
private class FakeUploadRepository : ReceiverDeliveryDocumentUploadRepository {
    private val pending = ArrayDeque<CompletableDeferred<Result<String>>>()

    override suspend fun upload(
        bytes: ByteArray,
        extension: String,
    ): Result<String> {
        val deferred = CompletableDeferred<Result<String>>()
        pending.addLast(deferred)
        return deferred.await()
    }

    fun completeOldest(result: Result<String>) {
        pending.removeFirst().complete(result)
    }
}

/** [ReceiverAuthRepository] 가짜 — 제출 페이로드만 기록하고, 미지정 경로 호출은 error 로 드러낸다. */
private class FakeReceiverAuthRepository : ReceiverAuthRepository {
    val submittedPayloads = mutableListOf<Pair<String?, String?>>()

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> {
        submittedPayloads += deathCertificateUrl to familyRelationCertificateUrl
        return Result.success(
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

    override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> = error("verifyMasterKey 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun sendEmailAuthCode(email: String): Result<Unit> = error("sendEmailAuthCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult> = error("verifyEmailAuthCode 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl> = error("getPresignedUrl 은 이 시나리오에서 호출되면 안 됨")

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> =
        error("getDeliveryVerificationStatus 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> = error("getSenderMessage 는 이 시나리오에서 호출되면 안 됨")
}
