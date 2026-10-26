package com.afternote.feature.receiver.presentation.deliveryverification

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.testing.FakeIdentityVerificationRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.domain.usecase.SubmitDeliveryVerificationUseCase
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * 수신자 열람 신청 흐름의 실패 안내가 «팝업(서버 작업 실패)» 과 «스낵바(서버가 준 거절 사유)» 로
 * 갈리는지 고정한다 (#446).
 *
 * 갈래 자체보다 **재시도가 실패한 그 요청을 되짚는지** 가 이 테스트들의 관심사다 — 「다시 시도하기」
 * 가 닫기와 같아지거나 엉뚱한 요청을 부르는 것이 팝업 도입에서 가장 흔한 회귀다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeliveryVerificationFailurePopupTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `서류 업로드 실패는 스낵바가 아니라 업로드 팝업으로 간다`() {
        val upload = failingUploadRepository(IllegalStateException("S3 PUT 실패"))
        val viewModel = documentUploadViewModel(upload)

        viewModel.uploadDeathCertificate()

        val state = viewModel.uiState.value
        assertEquals(ReceiverErrorPopup.UPLOAD, state.errorPopup)
        assertNull(state.errorMessage)
    }

    @Test
    fun `업로드 팝업의 재시도는 고른 파일을 그대로 다시 올린다`() {
        val upload = failingUploadRepository(IllegalStateException("S3 PUT 실패"))
        val viewModel = documentUploadViewModel(upload)
        viewModel.uploadDeathCertificate()

        viewModel.retryFailedRequest()

        assertEquals(2, upload.uploadCalls.size)
        assertEquals(
            DOCUMENT_BYTES.toList(),
            upload.uploadCalls
                .last()
                .bytes
                .toList(),
        )
    }

    @Test
    fun `업로드 팝업을 닫으면 붙들고 있던 재시도도 함께 버려진다`() {
        val upload = failingUploadRepository(IllegalStateException("S3 PUT 실패"))
        val viewModel = documentUploadViewModel(upload)
        viewModel.uploadDeathCertificate()

        viewModel.onErrorPopupDismissed()
        viewModel.retryFailedRequest()

        assertNull(viewModel.uiState.value.errorPopup)
        assertEquals(1, upload.uploadCalls.size)
    }

    @Test
    fun `서버가 준 거절 사유는 팝업이 아니라 문구로 남는다`() {
        val auth =
            submittingAuth {
                Result.failure(
                    ReceiverFailure.UserRejection(
                        reason = ReceiverRejectionReason.VERIFICATION_ALREADY_SUBMITTED,
                        cause = CAUSE,
                    ),
                )
            }
        val viewModel = submittableViewModel(auth)

        viewModel.submit()

        val state = viewModel.uiState.value
        assertNull(state.errorPopup)
        assertEquals(UiText.Resource(R.string.receiver_error_verification_already_submitted), state.errorMessage)
    }

    @Test
    fun `서버에 닿지 못한 제출은 네트워크 팝업으로 간다`() {
        val auth = submittingAuth { Result.failure(ReceiverFailure.NetworkUnavailable(CAUSE)) }
        val viewModel = submittableViewModel(auth)

        viewModel.submit()

        assertEquals(ReceiverErrorPopup.NETWORK, viewModel.uiState.value.errorPopup)
    }

    @Test
    fun `서버 오류 팝업의 재시도는 같은 신청을 다시 보낸다`() {
        val auth = submittingAuth { Result.failure(ReceiverFailure.UnexpectedServerFailure(CAUSE)) }
        val viewModel = submittableViewModel(auth)
        viewModel.submit()
        assertEquals(ReceiverErrorPopup.SERVER, viewModel.uiState.value.errorPopup)

        viewModel.retryFailedRequest()

        assertEquals(listOf(UPLOADED_URL to null, UPLOADED_URL to null), auth.deliverySubmissions)
    }

    /** 발송과 검증을 한 재시도로 뭉뚱그리면 검증 실패가 인증번호를 다시 보내는 회귀가 난다. */
    @Test
    fun `인증번호 검증 실패의 재시도는 검증만 다시 부른다`() {
        val auth =
            FakeReceiverAuthRepository.strict().apply {
                onSendEmailAuthCode = { Result.success(Unit) }
                onVerifyEmailAuthCode = { _, _ ->
                    Result.failure(ReceiverFailure.UnexpectedServerFailure(CAUSE))
                }
            }
        val viewModel = IdentityVerificationViewModel(auth, FakeIdentityVerificationRepository(), NoopErrorReporter)
        viewModel.onEmailChange(EMAIL)
        viewModel.requestVerificationCode()
        viewModel.onCodeChange("123456")
        viewModel.verifyAndProceed(SENDER_ID)
        assertEquals(ReceiverErrorPopup.SERVER, viewModel.uiState.value.errorPopup)

        viewModel.retryFailedRequest()

        assertEquals(1, auth.sentEmails.size)
        assertEquals(2, auth.verifiedEmailCodes.size)
    }

    @Test
    fun `이메일 미등록 거절은 팝업 없이 문구로 남는다`() {
        val auth =
            FakeReceiverAuthRepository.strict().apply {
                onSendEmailAuthCode = {
                    Result.failure(
                        ReceiverFailure.UserRejection(
                            reason = ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND,
                            cause = CAUSE,
                        ),
                    )
                }
            }
        val viewModel = IdentityVerificationViewModel(auth, FakeIdentityVerificationRepository(), NoopErrorReporter)
        viewModel.onEmailChange(EMAIL)

        viewModel.requestVerificationCode()

        val state = viewModel.uiState.value
        assertNull(state.errorPopup)
        assertEquals(UiText.Resource(R.string.receiver_error_email_not_found), state.errorMessage)
    }

    private fun documentUploadViewModel(
        upload: FakeReceiverDeliveryDocumentUploadRepository,
        auth: FakeReceiverAuthRepository = FakeReceiverAuthRepository.strict(),
    ): DocumentUploadViewModel =
        DocumentUploadViewModel(
            uploadRepository = upload,
            submitDeliveryVerification = SubmitDeliveryVerificationUseCase(auth),
            errorReporter = NoopErrorReporter,
        )

    /** 사망진단서 한 장이 이미 올라간 상태 — 제출 실패 갈래를 보려면 여기부터 시작해야 한다. */
    private fun submittableViewModel(auth: FakeReceiverAuthRepository): DocumentUploadViewModel {
        val upload =
            FakeReceiverDeliveryDocumentUploadRepository.strict().apply {
                onUpload = { _, _ -> Result.success(UPLOADED_URL) }
            }
        return documentUploadViewModel(upload, auth).apply { uploadDeathCertificate() }
    }

    private fun DocumentUploadViewModel.uploadDeathCertificate() {
        uploadDocument(
            slot = DocumentSlot.DeathCertificate,
            bytes = DOCUMENT_BYTES,
            extension = "pdf",
            displayName = "사망진단서.pdf",
        )
    }

    private fun failingUploadRepository(failure: Throwable): FakeReceiverDeliveryDocumentUploadRepository =
        FakeReceiverDeliveryDocumentUploadRepository.strict().apply {
            onUpload = { _, _ -> Result.failure(failure) }
        }

    private fun submittingAuth(onSubmit: suspend () -> Result<DeliveryVerification>): FakeReceiverAuthRepository =
        FakeReceiverAuthRepository.strict().apply {
            onSubmitDeliveryVerification = { _, _ -> onSubmit() }
        }

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private companion object {
        val DOCUMENT_BYTES = byteArrayOf(1, 2, 3)
        const val UPLOADED_URL = "https://cdn.test/death.pdf"
        const val EMAIL = "receiver@example.test"
        const val SENDER_ID = "sender-1"

        /**
         * 실패가 나르는 원인 예외 자리. 프로덕션에서는 `ApiException` 이 들어오지만 도메인 계약이
         * 요구하는 것은 `Throwable` 뿐이라 core:network 를 끌어오지 않는다.
         */
        val CAUSE: Throwable = IOException("stub cause")
    }
}
