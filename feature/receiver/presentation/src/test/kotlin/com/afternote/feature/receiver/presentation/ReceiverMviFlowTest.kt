package com.afternote.feature.receiver.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.testing.FakeIdentityVerificationRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.receiver.domain.usecase.SubmitDeliveryVerificationUseCase
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentSlot
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadIntent
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadScreen
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyScreen
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyViewModel
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsScreen
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsViewModel
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationScreen
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationViewModel
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistry
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import com.afternote.feature.receiver.presentation.R as ReceiverR

/** 실제 Screen → Intent → 저장소 → 소비 경로. 공개 VM에 기대던 앱 테스트를 소유 모듈로 옮겼다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverMviFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun emailCodeExpired_resendAndNewCode_verifyExactlyOnce() {
        val verifyEmailResults = ArrayDeque<Result<ReceiverEmailAuthResult>>()
        val authRepository =
            FakeReceiverAuthRepository.strict().apply {
                onSendEmailAuthCode = { Result.success(Unit) }
                onVerifyEmailAuthCode = { _, _ -> verifyEmailResults.removeFirst() }
            }
        verifyEmailResults.addLast(
            Result.failure(
                ReceiverFailure.UserRejection(
                    reason = ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND,
                    cause = CAUSE,
                ),
            ),
        )
        verifyEmailResults.addLast(
            Result.success(ReceiverEmailAuthResult(7L, "김수신", "이발신")),
        )
        val identityRepository = FakeIdentityVerificationRepository()
        val reporter = FakeErrorReporter()
        val viewModel =
            IdentityVerificationViewModel(
                authRepository,
                identityRepository,
                reporter,
            )
        var verifiedTransitions = 0

        composeRule.setContent {
            AfternoteTheme {
                IdentityVerificationEmailScreen(
                    senderId = "sender-1",
                    onBackClick = {},
                    onVerified = { verifiedTransitions += 1 },
                    viewModel = viewModel,
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_verify_email_placeholder))
            .performTextInput("receiver@example.test")
        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_verify_request_code))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_verify_code_placeholder))
            .performTextInput("123456")
        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_verify_next_button))
            .performClick()
        composeRule
            .onNodeWithText("인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_verify_request_code))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { authRepository.sentEmails.size == 2 }
        composeRule
            .onNode(hasSetTextAction() and hasText("123456"))
            .performTextReplacement("654321")
        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_verify_next_button))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { verifiedTransitions == 1 }

        assertEquals(
            listOf("receiver@example.test", "receiver@example.test"),
            authRepository.sentEmails,
        )
        assertEquals(
            listOf(
                "receiver@example.test" to "123456",
                "receiver@example.test" to "654321",
            ),
            authRepository.verifiedEmailCodes,
        )
        assertEquals(listOf("sender-1"), identityRepository.markVerifiedSenderIds)
        assertEquals(1, verifiedTransitions)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun documentSubmit_doubleTapWhileRequestInFlight_sendsOnePayload() {
        val uploadRepository =
            FakeReceiverDeliveryDocumentUploadRepository(
                defaultFileUrl = "https://cdn.example.test/death.pdf",
            )
        val pendingSubmission = CompletableDeferred<Result<DeliveryVerification>>()
        val authRepository =
            FakeReceiverAuthRepository.strict().apply {
                onSubmitDeliveryVerification = { _, _ -> pendingSubmission.await() }
            }
        val viewModel =
            DocumentUploadViewModel(
                uploadRepository,
                SubmitDeliveryVerificationUseCase(authRepository),
                FakeErrorReporter(),
            )
        composeRule.setContent { AfternoteTheme {} }

        composeRule.runOnIdle {
            viewModel.onIntent(
                DocumentUploadIntent.UploadDocument(
                    slot = DocumentSlot.DeathCertificate,
                    bytes = byteArrayOf(1, 2, 3),
                    extension = "pdf",
                    displayName = "사망진단서.pdf",
                ),
            )
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.canSubmit }

        composeRule.runOnIdle {
            viewModel.onIntent(DocumentUploadIntent.Submit)
            viewModel.onIntent(DocumentUploadIntent.Submit)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { authRepository.deliverySubmissions.size == 1 }

        assertTrue(viewModel.uiState.value.isSubmitting)
        assertEquals(
            listOf("https://cdn.example.test/death.pdf" to null),
            authRepository.deliverySubmissions,
        )
        assertEquals(1, uploadRepository.uploadCalls.size)

        pendingSubmission.complete(
            Result.success(
                DeliveryVerification(
                    id = 11L,
                    status = DeliveryVerificationStatus.PENDING,
                    deathCertificateUrl = "https://cdn.example.test/death.pdf",
                    familyRelationCertificateUrl = null,
                    adminNote = null,
                    createdAt = null,
                ),
            ),
        )
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.isSubmitted }
        assertEquals(1, authRepository.deliverySubmissions.size)
    }

    @Test
    fun documentSlot_replaceFailureKeepsPreviousThenSuccessReflectsReplacement() {
        val uploadResults = ArrayDeque<Result<String>>()
        val uploadRepository =
            FakeReceiverDeliveryDocumentUploadRepository.strict().apply {
                onUpload = { _, _ -> uploadResults.removeFirst() }
            }
        uploadResults.addLast(Result.success("https://cdn.example.test/original.pdf"))
        uploadResults.addLast(Result.failure(IllegalStateException("replacement failed")))
        uploadResults.addLast(Result.success("https://cdn.example.test/replacement.pdf"))
        val viewModel =
            DocumentUploadViewModel(
                uploadRepository,
                SubmitDeliveryVerificationUseCase(FakeReceiverAuthRepository.strict()),
                FakeErrorReporter(),
            )
        composeRule.setContent {
            AfternoteTheme {
                DocumentUploadScreen(
                    onBackClick = {},
                    onSubmitted = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.runOnIdle {
            viewModel.onIntent(
                DocumentUploadIntent.UploadDocument(
                    DocumentSlot.DeathCertificate,
                    byteArrayOf(1),
                    "pdf",
                    "원본 사망진단서.pdf",
                ),
            )
        }
        composeRule.onNodeWithText("원본 사망진단서.pdf").assertIsDisplayed()

        composeRule.runOnIdle {
            viewModel.onIntent(
                DocumentUploadIntent.UploadDocument(
                    DocumentSlot.DeathCertificate,
                    byteArrayOf(2),
                    "pdf",
                    "실패한 교체본.pdf",
                ),
            )
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.deathCertificate.isUploading
        }
        composeRule.onNodeWithText("원본 사망진단서.pdf").assertIsDisplayed()
        assertEquals(
            "https://cdn.example.test/original.pdf",
            viewModel.uiState.value.deathCertificate.fileUrl,
        )

        composeRule.runOnIdle {
            viewModel.onIntent(DocumentUploadIntent.ConsumeError)
            viewModel.onIntent(
                DocumentUploadIntent.UploadDocument(
                    DocumentSlot.DeathCertificate,
                    byteArrayOf(3),
                    "pdf",
                    "교체 사망진단서.pdf",
                ),
            )
        }
        composeRule.onNodeWithText("교체 사망진단서.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("원본 사망진단서.pdf").assertDoesNotExist()
        assertEquals(
            "https://cdn.example.test/replacement.pdf",
            viewModel.uiState.value.deathCertificate.fileUrl,
        )
        assertEquals(3, uploadRepository.uploadCalls.size)
    }

    @Test
    fun senderRegistration_recordsEntryAndMasterKey_shareExactReceiverContext() {
        val senderRegistry = SenderRegistry()
        val registrationViewModel = SenderRegistrationViewModel(senderRegistry)
        val recordsViewModel = ReceivedRecordsViewModel(senderRegistry)
        val masterKeyResults = ArrayDeque<Result<ReceiverIdentity>>()
        val receiverRepository =
            FakeReceiverRepository.strict().apply {
                onSaveMasterKey = { masterKeyState.value = it }
            }
        val authRepository =
            FakeReceiverAuthRepository.strict().apply {
                onVerifyMasterKey = { masterKeyResults.removeFirst() }
            }
        masterKeyResults.addLast(
            Result.success(
                ReceiverIdentity(
                    receiverId = 7L,
                    receiverName = "김수신",
                    senderName = "이발신",
                    relation = "가족",
                ),
            ),
        )
        val masterKeyViewModel =
            MasterKeyViewModel(
                senderRegistry = senderRegistry,
                receiverRepository = receiverRepository,
                receiverAuthRepository = authRepository,
                errorReporter = FakeErrorReporter(),
            )
        var phase by mutableStateOf(RegistrationPhase.REGISTRATION)
        var selectedSenderId: String? = null
        var registeredTransitions = 0
        var verifiedTransitions = 0

        composeRule.setContent {
            AfternoteTheme {
                when (phase) {
                    RegistrationPhase.REGISTRATION -> {
                        SenderRegistrationScreen(
                            onBackClick = {},
                            onRegistered = {
                                registeredTransitions += 1
                                phase = RegistrationPhase.RECORDS
                            },
                            viewModel = registrationViewModel,
                        )
                    }

                    RegistrationPhase.RECORDS -> {
                        ReceivedRecordsScreen(
                            onBackClick = {},
                            onAddSenderClick = {},
                            onSenderClick = { sender ->
                                selectedSenderId = sender.id
                                phase = RegistrationPhase.MASTER_KEY
                            },
                            viewModel = recordsViewModel,
                        )
                    }

                    RegistrationPhase.MASTER_KEY -> {
                        MasterKeyScreen(
                            senderId = checkNotNull(selectedSenderId),
                            onBackClick = {},
                            onVerified = { verifiedTransitions += 1 },
                            viewModel = masterKeyViewModel,
                        )
                    }
                }
            }
        }

        val registerButton = hasText("발신자 등록하기") and hasClickAction()
        composeRule.onNode(registerButton).assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextInput("  가족 별칭  ")
        composeRule.onNode(registerButton).assertIsEnabled().performClick()

        composeRule.onNodeWithText("받은 기록함").assertIsDisplayed()
        composeRule.onNodeWithText("가족 별칭").assertIsDisplayed().performClick()
        val sender = senderRegistry.senders.value.single()
        assertEquals(1, registeredTransitions)
        assertEquals("가족 별칭", sender.name)
        assertEquals(sender.id, selectedSenderId)

        composeRule
            .onNode(hasText("마스터 키 입력") and hasSetTextAction())
            .assertIsDisplayed()
        val nextButton = hasText("다음") and hasClickAction()
        composeRule.onNode(nextButton).assertIsNotEnabled()
        // 마스터 키는 UUID 형식만 통과하고 소문자로 정규화된다(#887) — 공백·대문자 입력으로 함께 검증.
        composeRule.onNode(hasSetTextAction()).performTextInput("  3F2504E0-4F89-11D3-9A0C-0305E82C3301  ")
        composeRule.onNode(nextButton).assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { verifiedTransitions == 1 }

        val normalizedMasterKey = "3f2504e0-4f89-11d3-9a0c-0305e82c3301"
        val attached = checkNotNull(senderRegistry.findById(sender.id))
        assertEquals(listOf(normalizedMasterKey), authRepository.verifiedMasterKeys)
        assertEquals(listOf(normalizedMasterKey), receiverRepository.savedMasterKeys)
        assertEquals(normalizedMasterKey, receiverRepository.masterKeyState.value)
        assertEquals(normalizedMasterKey, attached.masterKey)
        assertEquals("이발신", attached.realSenderName)
        assertEquals("가족", attached.relation)
        assertEquals(1, verifiedTransitions)
    }

    private companion object {
        val CAUSE: Throwable = IOException("stub cause")
    }
}

private enum class RegistrationPhase {
    REGISTRATION,
    RECORDS,
    MASTER_KEY,
}

private class FakeErrorReporter : ErrorReporter {
    val failures = mutableListOf<Pair<Throwable, Map<String, String>>>()

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        failures += throwable to attributes
    }
}
