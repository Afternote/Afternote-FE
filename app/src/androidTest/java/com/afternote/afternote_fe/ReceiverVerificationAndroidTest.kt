package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.testing.FakeIdentityVerificationRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentSlot
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ReceiverVerificationAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun emailMismatchThenRetry_preservesContextAndVerifiesOnce() {
        val verifyEmailResults = ArrayDeque<Result<ReceiverEmailAuthResult>>()
        val auth =
            FakeReceiverAuthRepository.strict().apply {
                onSendEmailAuthCode = { Result.success(Unit) }
                onVerifyEmailAuthCode = { _, _ -> verifyEmailResults.removeFirst() }
            }
        verifyEmailResults.addLast(
            Result.failure(
                ReceiverFailure.ServerRejection(
                    status = 400,
                    serverMessage = "인증번호가 일치하지 않습니다.",
                    serverCode = 1903,
                    cause = CAUSE,
                ),
            ),
        )
        verifyEmailResults.addLast(
            Result.success(ReceiverEmailAuthResult(7L, "김수신", "이발신")),
        )
        val identity = FakeIdentityVerificationRepository()
        val viewModel = IdentityVerificationViewModel(auth, identity, FakeErrorReporter())
        var verifiedCalls = 0
        composeRule.setContent {
            AfternoteTheme {
                IdentityVerificationEmailScreen(
                    onBackClick = {},
                    onVerified = { verifiedCalls += 1 },
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("이메일 주소").performTextInput("receiver@example.test")
        composeRule.onNodeWithText("인증번호 받기").performClick()
        composeRule.onNodeWithText("인증번호가 전송되었습니다.\n수신 된 인증번호를 입력해 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("인증번호").performTextInput("123456")
        composeRule.onNodeWithText("다음").performClick()
        composeRule.onNodeWithText("인증번호가 일치하지 않습니다.").assertIsDisplayed()

        composeRule.onNodeWithText("다음").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { verifiedCalls == 1 }

        assertEquals(listOf("receiver@example.test"), auth.sentEmails)
        assertEquals(
            listOf(
                "receiver@example.test" to "123456",
                "receiver@example.test" to "123456",
            ),
            auth.verifiedEmailCodes,
        )
        assertEquals(1, identity.markVerifiedCallCount)
        assertEquals(1, verifiedCalls)
    }

    @Test
    fun invalidEmail_doesNotCallRepositoryAndShowsSemantics() {
        val auth = FakeReceiverAuthRepository.strict()
        val viewModel =
            IdentityVerificationViewModel(
                auth,
                FakeIdentityVerificationRepository(),
                FakeErrorReporter(),
            )
        composeRule.setContent {
            AfternoteTheme {
                IdentityVerificationEmailScreen({}, {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("이메일 주소").performTextInput("invalid-email")
        composeRule.onNodeWithText("이메일 형식이 올바르지 않습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("인증번호 받기").performClick()

        assertTrue(auth.sentEmails.isEmpty())
    }

    @Test
    fun documentFailureThenRetry_submitsOnlySuccessfulUrlOnce() {
        val uploadResults = ArrayDeque<Result<String>>()
        val upload =
            FakeReceiverDeliveryDocumentUploadRepository.strict().apply {
                onUpload = { _, _ -> uploadResults.removeFirst() }
            }
        uploadResults.addLast(Result.failure(IllegalStateException("upload failed")))
        uploadResults.addLast(Result.success("https://cdn.test/death.pdf"))
        val auth =
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
        val viewModel = DocumentUploadViewModel(upload, auth, FakeErrorReporter())
        composeRule.setContent { AfternoteTheme {} }

        composeRule.runOnIdle {
            viewModel.uploadDocument(DocumentSlot.DeathCertificate, byteArrayOf(1), "pdf", "사망진단서.pdf")
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.uiState.value.deathCertificate.isUploading
        }
        assertFalse(viewModel.uiState.value.canSubmit)

        composeRule.runOnIdle {
            viewModel.uploadDocument(DocumentSlot.DeathCertificate, byteArrayOf(1), "pdf", "사망진단서.pdf")
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.canSubmit }
        composeRule.runOnIdle { viewModel.submit() }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.isSubmitted }

        assertEquals(listOf("https://cdn.test/death.pdf" to null), auth.deliverySubmissions)
        assertEquals(2, upload.uploadCalls.size)
    }
}

/**
 * ServerRejection 이 나르는 원인 예외 자리. 프로덕션에서는 `ApiException` 이 들어오지만, 도메인 계약이
 * 요구하는 것은 `Throwable` 뿐이라 이 테스트들은 core:network 를 끌어오지 않는다.
 */
private val CAUSE: Throwable = IOException("stub cause")
