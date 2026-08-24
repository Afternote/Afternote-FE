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
import com.afternote.feature.receiver.domain.error.ReceiverEmailAuthException
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentSlot
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        val auth = FakeReceiverAuthRepository()
        auth.verifyEmailResults.addLast(
            Result.failure(
                ReceiverEmailAuthException(
                    status = 400,
                    serverMessage = "인증번호가 일치하지 않습니다.",
                    serverCode = 1903,
                ),
            ),
        )
        auth.verifyEmailResults.addLast(
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
        assertEquals(1, identity.markVerifiedCalls)
        assertEquals(1, verifiedCalls)
    }

    @Test
    fun invalidEmail_doesNotCallRepositoryAndShowsSemantics() {
        val auth = FakeReceiverAuthRepository()
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
        val upload = FakeDocumentUploadRepository()
        upload.results.addLast(Result.failure(IllegalStateException("upload failed")))
        upload.results.addLast(Result.success("https://cdn.test/death.pdf"))
        val auth = FakeReceiverAuthRepository()
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
        assertEquals(2, upload.calls)
    }
}

private class FakeIdentityVerificationRepository : IdentityVerificationRepository {
    private val state = MutableStateFlow(false)
    override val isVerified: Flow<Boolean> = state
    var markVerifiedCalls = 0

    override suspend fun markVerified() {
        markVerifiedCalls += 1
        state.value = true
    }
}

private class FakeDocumentUploadRepository : ReceiverDeliveryDocumentUploadRepository {
    val results = ArrayDeque<Result<String>>()
    var calls = 0

    override suspend fun upload(
        bytes: ByteArray,
        extension: String,
    ): Result<String> {
        calls += 1
        return results.removeFirst()
    }
}

private class FakeReceiverAuthRepository : ReceiverAuthRepository {
    val sentEmails = mutableListOf<String>()
    val verifiedEmailCodes = mutableListOf<Pair<String, String>>()
    val verifyEmailResults = ArrayDeque<Result<ReceiverEmailAuthResult>>()
    val deliverySubmissions = mutableListOf<Pair<String?, String?>>()

    override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> = error("unexpected verifyMasterKey")

    override suspend fun sendEmailAuthCode(email: String): Result<Unit> {
        sentEmails += email
        return Result.success(Unit)
    }

    override suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult> {
        verifiedEmailCodes += email to authCode
        return verifyEmailResults.removeFirst()
    }

    override suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl> = error("unexpected getPresignedUrl")

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> {
        deliverySubmissions += deathCertificateUrl to familyRelationCertificateUrl
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

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> = error("unexpected getDeliveryVerificationStatus")

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> = error("unexpected getSenderMessage")
}
