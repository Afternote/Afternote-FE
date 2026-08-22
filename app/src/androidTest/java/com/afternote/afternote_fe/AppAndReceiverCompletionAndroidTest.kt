package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.screen.receiver.ReceiverHomeActions
import com.afternote.afternote_fe.screen.receiver.ReceiverHomeEvent
import com.afternote.afternote_fe.screen.receiver.ReceiverHomeScreen
import com.afternote.afternote_fe.screen.receiver.ReceiverHomeViewModel
import com.afternote.afternote_fe.screen.receiver.model.ReceiverHomeUiState
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeAuthRepository
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.InvalidLoginCredentialsException
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.error.ReceiverEmailAuthException
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistSong
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DocumentSlot
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DocumentUploadViewModel
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationViewModel
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.afternote.feature.afternote.presentation.R as AfternoteFeatureR
import com.afternote.feature.onboarding.presentation.R as OnboardingR

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AppAndReceiverCompletionAndroidTest {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var errorReporter: ErrorReporter

    private val fakeAuth get() = authRepository as FakeAuthRepository
    private val fakeErrorReporter get() = errorReporter as FakeErrorReporter

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun invalidCredentials_correctedPasswordThenRetry_entersHomeWithoutReportingUserError() {
        fakeAuth.emailLoginResults.addLast(
            Result.failure(InvalidLoginCredentialsException(IllegalStateException("rejected"))),
        )
        fakeAuth.emailLoginResults.addLast(
            Result.success(Session.DefaultSession("access", "refresh")),
        )

        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.welcome_start))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.login_email_label))
            .performTextInput("receiver@afternote.local")
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.login_password_label))
            .performTextInput("wrong-password")

        val loginButton =
            hasText(context.getString(OnboardingR.string.login_button)) and hasClickAction()
        composeRule.onNode(loginButton).performClick()
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.login_invalid_credentials))
            .assertIsDisplayed()

        composeRule
            .onNode(hasSetTextAction() and hasText("wrong-password"))
            .performTextReplacement("correct-password")
        composeRule.onNode(loginButton).performClick()

        val greeting = context.getString(R.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        composeRule.onNodeWithText(greeting).assertIsDisplayed()

        assertEquals(
            listOf(
                "receiver@afternote.local" to "wrong-password",
                "receiver@afternote.local" to "correct-password",
            ),
            fakeAuth.attemptedEmailLogins,
        )
        assertEquals(1, fakeAuth.saveSessionCalls)
        assertTrue(fakeErrorReporter.failures.isEmpty())
    }
}

@RunWith(AndroidJUnit4::class)
class ReceiverRuntimeCompletionAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun receiverHome_allFailureThenRetryPartialSuccess_keepsAvailableSectionsAndReportsBothStages() {
        val repository = CompletionReceiverRepository()
        val allFailureAttempt = repository.enqueueHomeAttempt()
        val partialAttempt = repository.enqueueHomeAttempt()
        val reporter = FakeErrorReporter()
        val viewModel = ReceiverHomeViewModel(repository, reporter)

        composeRule.setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            AfternoteTheme {
                ReceiverHomeScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    actions = ReceiverHomeActions.Noop,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { repository.homeCallCounts.all { it == 1 } }
        composeRule.runOnIdle {
            assertSame(ReceiverHomeUiState.Loading, viewModel.uiState.value)
        }

        val offline = IllegalStateException("offline")
        allFailureAttempt.complete(
            afterNotes = Result.failure(offline),
            mindRecords = Result.failure(offline),
            timeLetters = Result.failure(offline),
            senderMessage = Result.failure(offline),
        )
        composeRule
            .onNodeWithText(context.getString(R.string.home_tab_error_message))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.home_tab_retry))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { repository.homeCallCounts.all { it == 2 } }
        composeRule.runOnIdle {
            assertSame(ReceiverHomeUiState.Loading, viewModel.uiState.value)
        }

        partialAttempt.complete(
            afterNotes =
                Result.success(
                    AfterNotesListResult(
                        items =
                            listOf(
                                AfterNoteListItem(1L, "Google Drive", AfternoteType.GALLERY_AND_FILES, null),
                                AfterNoteListItem(2L, "추억 노트", AfternoteType.MEMORIAL, null),
                            ),
                        totalCount = 2,
                    ),
                ),
            mindRecords = Result.failure(IllegalStateException("mind records unavailable")),
            timeLetters = Result.success(LoadCountResult(totalCount = 8)),
            senderMessage =
                Result.success(
                    SenderMessageInfo(
                        senderName = "이발신",
                        message = "언제나 응원할게",
                        createdAt = "2026.08.22",
                    ),
                ),
        )

        composeRule
            .onNodeWithText(context.getString(R.string.receiver_home_sender_record_title, "이발신"))
            .assertIsDisplayed()
        composeRule.onNodeWithText("언제나 응원할게").assertIsDisplayed()
        composeRule
            .onNodeWithText("0개 마음의 기록이 있습니다.")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("2개의 애프터노트가 있습니다.")
            .performScrollTo()
            .assertIsDisplayed()

        assertEquals(2, reporter.failures.size)
        assertEquals("receiver_home_load", reporter.failures[0].second["home_stage"])
        assertEquals("receiver_home_partial_load", reporter.failures[1].second["home_stage"])
        assertEquals("mind_records", reporter.failures[1].second["home_failed_sources"])
        assertEquals(listOf(2, 2, 2, 2), repository.homeCallCounts)
    }

    @Test
    fun emailCodeExpired_resendAndNewCode_verifyExactlyOnce() {
        val authRepository = CompletionReceiverAuthRepository()
        authRepository.verifyEmailResults.addLast(
            Result.failure(
                ReceiverEmailAuthException(
                    status = 400,
                    serverMessage = "인증번호가 만료되었습니다. 다시 발급해 주세요.",
                    serverCode = 1902,
                ),
            ),
        )
        authRepository.verifyEmailResults.addLast(
            Result.success(ReceiverEmailAuthResult(7L, "김수신", "이발신")),
        )
        val identityRepository = CompletionIdentityVerificationRepository()
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
                    onBackClick = {},
                    onVerified = { verifiedTransitions += 1 },
                    viewModel = viewModel,
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_verify_email_placeholder))
            .performTextInput("receiver@example.test")
        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_verify_request_code))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_verify_code_placeholder))
            .performTextInput("123456")
        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_verify_next_button))
            .performClick()
        composeRule
            .onNodeWithText("인증번호가 만료되었습니다. 다시 발급해 주세요.")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_verify_request_code))
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { authRepository.sentEmails.size == 2 }
        composeRule
            .onNode(hasSetTextAction() and hasText("123456"))
            .performTextReplacement("654321")
        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_verify_next_button))
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
        assertEquals(1, identityRepository.markVerifiedCalls)
        assertEquals(1, verifiedTransitions)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun documentSubmit_doubleTapWhileRequestInFlight_sendsOnePayload() {
        val uploadRepository = CompletionDocumentUploadRepository()
        val authRepository = CompletionReceiverAuthRepository()
        val pendingSubmission = authRepository.enqueueSubmission()
        val viewModel =
            DocumentUploadViewModel(
                uploadRepository,
                authRepository,
                FakeErrorReporter(),
            )
        composeRule.setContent { AfternoteTheme {} }

        composeRule.runOnIdle {
            viewModel.uploadDocument(
                slot = DocumentSlot.DeathCertificate,
                bytes = byteArrayOf(1, 2, 3),
                extension = "pdf",
                displayName = "사망진단서.pdf",
            )
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.canSubmit }

        composeRule.runOnIdle {
            viewModel.submit()
            viewModel.submit()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { authRepository.deliverySubmissions.size == 1 }

        assertTrue(viewModel.uiState.value.isSubmitting)
        assertEquals(
            listOf("https://cdn.example.test/death.pdf" to null),
            authRepository.deliverySubmissions,
        )
        assertEquals(1, uploadRepository.calls)

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
    fun receivedGalleryDetail_routesGalleryContractInsteadOfSocialCredentials() {
        val repository = CompletionReceiverRepository()
        repository.detailResults.addLast(
            Result.success(
                ReceivedAfternoteDetail(
                    title = "Google Drive",
                    senderName = "이발신",
                    createdAt = "2026.08.22",
                    category = "GALLERY",
                    type = AfternoteType.GALLERY_AND_FILES,
                    processingMethods = listOf("가족에게 폴더 전달"),
                    leaveMessageBlocks =
                        listOf(LeaveMessageBlock(title = "사진", body = "여행 사진을 보관해 줘")),
                ),
            ),
        )
        val viewModel =
            ReceivedAfternoteDetailViewModel(
                savedStateHandle = SavedStateHandle(mapOf("afternoteId" to "202")),
                receiverRepository = repository,
                errorReporter = FakeErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                ReceivedAfternoteDetailRoute(onBack = {}, viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("Google Drive").assertIsDisplayed()
        composeRule.onNodeWithText("가족에게 폴더 전달").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("여행 사진을 보관해 줘").performScrollTo().assertIsDisplayed()
        assertEquals(listOf(202L), repository.detailIds)
    }

    @Test
    fun receivedMemorialDetail_routesPlaylistContractAndForwardsExactId() {
        val repository = CompletionReceiverRepository()
        repository.detailResults.addLast(
            Result.success(
                ReceivedAfternoteDetail(
                    title = "추억 노트",
                    senderName = "이발신",
                    category = "PLAYLIST",
                    type = AfternoteType.MEMORIAL,
                    leaveMessageBlocks =
                        listOf(LeaveMessageBlock(title = "마지막 말", body = "이 노래들을 기억해 줘")),
                    playlist =
                        ReceivedPlaylistDetail(
                            songs =
                                listOf(
                                    ReceivedPlaylistSong("첫 번째 노래", "가수 A", null),
                                    ReceivedPlaylistSong("두 번째 노래", "가수 B", null),
                                ),
                        ),
                ),
            ),
        )
        val viewModel =
            ReceivedAfternoteDetailViewModel(
                savedStateHandle = SavedStateHandle(mapOf("afternoteId" to "303")),
                receiverRepository = repository,
                errorReporter = FakeErrorReporter(),
            )
        val playlistRoutes = mutableListOf<String>()

        composeRule.setContent {
            AfternoteTheme {
                ReceivedAfternoteDetailRoute(
                    onBack = {},
                    onNavigateToPlaylist = playlistRoutes::add,
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("故 이발신님의 애프터노트").assertIsDisplayed()
        composeRule
            .onNodeWithText("현재 2개의 노래가 담겨 있습니다.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNode(hasText("추억 플레이리스트") and hasClickAction())
            .performClick()

        assertEquals(listOf("303"), playlistRoutes)
        assertEquals(listOf(303L), repository.detailIds)
    }
}

private data class PendingHomeAttempt(
    val afterNotes: CompletableDeferred<Result<AfterNotesListResult>>,
    val mindRecords: CompletableDeferred<Result<LoadCountResult>>,
    val timeLetters: CompletableDeferred<Result<LoadCountResult>>,
    val senderMessage: CompletableDeferred<Result<SenderMessageInfo?>>,
) {
    fun complete(
        afterNotes: Result<AfterNotesListResult>,
        mindRecords: Result<LoadCountResult>,
        timeLetters: Result<LoadCountResult>,
        senderMessage: Result<SenderMessageInfo?>,
    ) {
        this.afterNotes.complete(afterNotes)
        this.mindRecords.complete(mindRecords)
        this.timeLetters.complete(timeLetters)
        this.senderMessage.complete(senderMessage)
    }
}

private class CompletionReceiverRepository : ReceiverRepository {
    private val authCode = MutableStateFlow<String?>(null)
    override val authCodeFlow: Flow<String?> = authCode

    private val afterNoteHomeResults = ArrayDeque<CompletableDeferred<Result<AfterNotesListResult>>>()
    private val mindRecordHomeResults = ArrayDeque<CompletableDeferred<Result<LoadCountResult>>>()
    private val timeLetterHomeResults = ArrayDeque<CompletableDeferred<Result<LoadCountResult>>>()
    private val senderMessageHomeResults = ArrayDeque<CompletableDeferred<Result<SenderMessageInfo?>>>()

    val detailResults = ArrayDeque<Result<ReceivedAfternoteDetail>>()
    val detailIds = mutableListOf<Long>()
    val homeCallCounts = mutableListOf(0, 0, 0, 0)

    fun enqueueHomeAttempt(): PendingHomeAttempt {
        val attempt =
            PendingHomeAttempt(
                afterNotes = CompletableDeferred(),
                mindRecords = CompletableDeferred(),
                timeLetters = CompletableDeferred(),
                senderMessage = CompletableDeferred(),
            )
        afterNoteHomeResults.addLast(attempt.afterNotes)
        mindRecordHomeResults.addLast(attempt.mindRecords)
        timeLetterHomeResults.addLast(attempt.timeLetters)
        senderMessageHomeResults.addLast(attempt.senderMessage)
        return attempt
    }

    override suspend fun currentAuthCode(): String? = authCode.value

    override suspend fun saveAuthCode(code: String) {
        authCode.value = code
    }

    override suspend fun clearAuthCode() {
        authCode.value = null
    }

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> = flowOf(PagingData.empty())

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> {
        homeCallCounts[0] += 1
        return afterNoteHomeResults.removeFirst().await()
    }

    override suspend fun getReceivedAfternoteDetail(afternoteId: Long): Result<ReceivedAfternoteDetail> {
        detailIds += afternoteId
        return detailResults.removeFirst()
    }

    override suspend fun downloadReceivedExport(): Result<ReceivedExportBundle> {
        error("unexpected downloadReceivedExport")
    }

    override suspend fun saveReceivedExportToFile(bundle: ReceivedExportBundle): Result<Unit> {
        error("unexpected saveReceivedExportToFile")
    }

    override suspend fun loadMindRecordsCount(): Result<LoadCountResult> {
        homeCallCounts[1] += 1
        return mindRecordHomeResults.removeFirst().await()
    }

    override suspend fun loadTimeLettersCount(): Result<LoadCountResult> {
        homeCallCounts[2] += 1
        return timeLetterHomeResults.removeFirst().await()
    }

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> {
        homeCallCounts[3] += 1
        return senderMessageHomeResults.removeFirst().await()
    }
}

private class CompletionIdentityVerificationRepository : IdentityVerificationRepository {
    private val verified = MutableStateFlow(false)
    override val isVerified: Flow<Boolean> = verified
    var markVerifiedCalls = 0

    override suspend fun markVerified() {
        markVerifiedCalls += 1
        verified.value = true
    }
}

private class CompletionDocumentUploadRepository : ReceiverDeliveryDocumentUploadRepository {
    var calls = 0

    override suspend fun upload(
        bytes: ByteArray,
        extension: String,
    ): Result<String> {
        calls += 1
        return Result.success("https://cdn.example.test/death.pdf")
    }
}

private class CompletionReceiverAuthRepository : ReceiverAuthRepository {
    val sentEmails = mutableListOf<String>()
    val verifiedEmailCodes = mutableListOf<Pair<String, String>>()
    val verifyEmailResults = ArrayDeque<Result<ReceiverEmailAuthResult>>()
    val deliverySubmissions = mutableListOf<Pair<String?, String?>>()
    private val submissionResults =
        ArrayDeque<CompletableDeferred<Result<DeliveryVerification>>>()

    fun enqueueSubmission(): CompletableDeferred<Result<DeliveryVerification>> =
        CompletableDeferred<Result<DeliveryVerification>>().also(submissionResults::addLast)

    override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> {
        error("unexpected verifyMasterKey")
    }

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

    override suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl> {
        error("unexpected getPresignedUrl")
    }

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> {
        deliverySubmissions += deathCertificateUrl to familyRelationCertificateUrl
        return submissionResults.removeFirst().await()
    }

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> {
        error("unexpected getDeliveryVerificationStatus")
    }

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> {
        error("unexpected getSenderMessage")
    }
}
