package com.afternote.afternote_fe

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.navigation.rememberAfternoteAppState
import com.afternote.afternote_fe.navigation.rememberHomeTabActions
import com.afternote.afternote_fe.navigation.rememberReceiverNavActions
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.afternote_fe.test.emptyWeeklyReport
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.model.Session
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel
import com.afternote.feature.home.presentation.HomeTabActions
import com.afternote.feature.home.presentation.receiver.ReceiverHomeActions
import com.afternote.feature.home.presentation.receiver.ReceiverHomeEvent
import com.afternote.feature.home.presentation.receiver.ReceiverHomeScreen
import com.afternote.feature.home.presentation.receiver.ReceiverHomeViewModel
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistDetail
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistSong
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.testing.FakeIdentityVerificationRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.domain.testing.FakeReceiverRepository
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentSlot
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadScreen
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationViewModel
import com.afternote.feature.receiver.presentation.navigation.ReceiverNavActions
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import javax.inject.Inject
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.afternote.presentation.R as AfternoteFeatureR
import com.afternote.feature.home.presentation.R as HomeR
import com.afternote.feature.onboarding.presentation.R as OnboardingR
import com.afternote.feature.receiver.presentation.R as ReceiverR

/** 이 테스트의 관심 밖인 외부 라우팅을 채우는 no-op 묶음. */
private val noopActions =
    ReceiverHomeActions(
        onNavigateToMindRecord = {},
        onNavigateToTimeLetter = {},
        onNavigateToAfternote = {},
    )

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AppAndReceiverCompletionAndroidTest {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var weeklyReportRepository: WeeklyReportRepository

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var userProfileRepository: UserProfileCacheRepository

    private val fakeAuth get() = authRepository as FakeAuthRepository
    private val fakeErrorReporter get() = errorReporter as FakeErrorReporter
    private val fakeWeeklyReport get() = weeklyReportRepository as FakeWeeklyReportRepository

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
        // 홈이 진입 시 주간 기록 수를 부른다 (#562). 정본 fake 는 큐가 비면 터뜨리므로,
        // 주간 수에 관심이 없는 이 테스트도 기대하는 응답을 명시적으로 넣는다 — 조용히 접으면
        // 요청 횟수가 어긋난 것을 놓친다.
        (weeklyReportRepository as FakeWeeklyReportRepository).results.addLast(
            Result.success(emptyWeeklyReport()),
        )
    }

    @Test
    fun invalidCredentials_correctedPasswordThenRetry_entersHomeWithoutReportingUserError() {
        val emailLoginResults = ArrayDeque<Result<Session.DefaultSession>>()
        emailLoginResults.addLast(
            Result.failure(CoreAuthFailure.InvalidLoginCredentials(IllegalStateException("rejected"))),
        )
        emailLoginResults.addLast(
            Result.success(Session.DefaultSession("access", "refresh")),
        )
        fakeAuth.onDefaultLogin = { _, _ ->
            requireNotNull(emailLoginResults.removeFirstOrNull()) { "email login 응답이 준비되지 않음" }
        }

        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_login_email_label))
            .performTextInput("receiver@afternote.local")
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_login_password_label))
            .performTextInput("wrong-password")

        val loginButton =
            hasText(context.getString(OnboardingR.string.onboarding_login_button)) and hasClickAction()
        composeRule.onNode(loginButton).performClick()
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_login_invalid_credentials))
            .assertIsDisplayed()

        composeRule
            .onNode(hasSetTextAction() and hasText("wrong-password"))
            .performTextReplacement("correct-password")
        composeRule.onNode(loginButton).performClick()

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        composeRule.onNodeWithText(greeting).assertIsDisplayed()

        runBlocking { userProfileRepository.savePasskeyRegistered(true) }
        val noteTabLabel = context.getString(CoreUiR.string.core_ui_nav_item_note)
        composeRule
            .onNode(
                hasText(noteTabLabel) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Selected, false),
            ).performClick()
        val fingerprintTitle =
            context.getString(AfternoteFeatureR.string.afternote_fingerprint_login_title)
        composeRule.waitUntilAtLeastOneExists(hasText(fingerprintTitle), timeoutMillis = 5_000)
        composeRule.onNodeWithText(fingerprintTitle).assertIsDisplayed()
        composeRule
            .onNode(
                hasText(noteTabLabel) and
                    SemanticsMatcher.expectValue(SemanticsProperties.Selected, true),
            ).assertIsSelected()

        assertEquals(
            listOf(
                "receiver@afternote.local" to "wrong-password",
                "receiver@afternote.local" to "correct-password",
            ),
            fakeAuth.attemptedEmailLogins,
        )
        assertEquals(1, fakeAuth.saveSessionCalls)
        assertEquals(0, fakeAuth.rotateTokenCalls)
        assertEquals(1, fakeWeeklyReport.requestedDates.size)
        assertTrue(fakeErrorReporter.failures.isEmpty())
    }

    @Test
    fun welcomeCheckRecords_opensActualReceivedRecordsStartDestination() {
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_check_records))
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_records_box_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_records_box_empty))
            .assertIsDisplayed()
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
        // 완료 시점을 테스트가 쥐어야 세 조회의 경합 순서를 만들 수 있다.
        val afterNoteHomeResults = ArrayDeque<CompletableDeferred<Result<AfterNotesListResult>>>()
        val senderMessageHomeResults = ArrayDeque<CompletableDeferred<Result<SenderMessageInfo?>>>()
        val repository =
            FakeReceiverRepository.strict().apply {
                onGetReceivedAfterNotes = { afterNoteHomeResults.removeFirst().await() }
                onLoadSenderMessage = { senderMessageHomeResults.removeFirst().await() }
            }
        val mindRecordHomeResults = ArrayDeque<CompletableDeferred<Result<ReceiverMindRecords>>>()
        val mindRecordRepository =
            FakeMindRecordReceiverRepository(onGetAll = { mindRecordHomeResults.removeFirst().await() })
        val timeLetterHomeResults = ArrayDeque<CompletableDeferred<Result<ReceivedTimeLetterList>>>()
        val timeLetterRepository =
            FakeReceiverTimeLetterRepository.strict().apply {
                onGetReceivedTimeLetters = {
                    timeLetterHomeResults.removeFirst().await().getOrThrow()
                }
            }

        fun homeCallCounts(): List<Int> =
            listOf(
                repository.getReceivedAfterNotesCalls,
                mindRecordRepository.getAllCalls,
                timeLetterRepository.getReceivedTimeLettersCalls,
                repository.loadSenderMessageCalls,
            )

        val allFailureAttempt =
            enqueueHomeAttempt(
                afterNoteHomeResults,
                mindRecordHomeResults,
                timeLetterHomeResults,
                senderMessageHomeResults,
            )
        val partialAttempt =
            enqueueHomeAttempt(
                afterNoteHomeResults,
                mindRecordHomeResults,
                timeLetterHomeResults,
                senderMessageHomeResults,
            )
        val reporter = FakeErrorReporter()
        val viewModel =
            ReceiverHomeViewModel(
                receiverRepository = repository,
                mindRecordReceiverRepository = mindRecordRepository,
                receiverTimeLetterRepository = timeLetterRepository,
                errorReporter = reporter,
            )

        composeRule.setContent {
            val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
            AfternoteTheme {
                ReceiverHomeScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    actions = noopActions,
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { homeCallCounts().all { it == 1 } }
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
            .onNodeWithText(context.getString(HomeR.string.home_receiver_error_message))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(HomeR.string.home_receiver_retry))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { homeCallCounts().all { it == 2 } }
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
            timeLetters = Result.success(ReceivedTimeLetterList(timeLetters = emptyList(), totalCount = 8)),
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
            .onNodeWithText(context.getString(HomeR.string.home_receiver_sender_record_title, "이발신"))
            .assertIsDisplayed()
        composeRule.onNodeWithText("언제나 응원할게").assertIsDisplayed()
        composeRule
            .onAllNodes(
                hasText(context.getString(HomeR.string.home_receiver_section_count_unavailable)),
            ).apply {
                assertCountEquals(2)
                this[0].performScrollTo().assertIsDisplayed()
            }
        composeRule
            .onNodeWithText("8개 라이프 이벤트 레터가 있습니다.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("2개의 애프터노트가 있습니다.")
            .performScrollTo()
            .assertIsDisplayed()

        assertEquals(2, reporter.failures.size)
        assertEquals("receiver_home_load", reporter.failures[0].second["receiver_stage"])
        assertEquals("receiver_home_partial_load", reporter.failures[1].second["receiver_stage"])
        assertEquals("mind_records", reporter.failures[1].second["receiver_failed_sources"])
        assertEquals(listOf(2, 2, 2, 2), homeCallCounts())
    }

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
                FakeReceiverAuthRepository.strict(),
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
            viewModel.uploadDocument(
                DocumentSlot.DeathCertificate,
                byteArrayOf(1),
                "pdf",
                "원본 사망진단서.pdf",
            )
        }
        composeRule.onNodeWithText("원본 사망진단서.pdf").assertIsDisplayed()

        composeRule.runOnIdle {
            viewModel.uploadDocument(
                DocumentSlot.DeathCertificate,
                byteArrayOf(2),
                "pdf",
                "실패한 교체본.pdf",
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
            viewModel.consumeError()
            viewModel.uploadDocument(
                DocumentSlot.DeathCertificate,
                byteArrayOf(3),
                "pdf",
                "교체 사망진단서.pdf",
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
    fun receiverVerificationActions_removeConsumedStepsAndCompletionReturnsToRecords() {
        var actions: ReceiverNavActions? = null
        composeRule.setContent {
            val appState = rememberAfternoteAppState()
            val receiverActions = rememberReceiverNavActions(appState)
            SideEffect { actions = receiverActions }
            AfternoteTheme {
                NavHost(
                    navController = appState.navController,
                    startDestination = ReceiverRoute.ReceivedRecordsRoute,
                ) {
                    composable<ReceiverRoute.ReceivedRecordsRoute> { Text("records") }
                    composable<ReceiverRoute.SenderDetailRoute> { Text("sender detail") }
                    navigation<ReceiverRoute.DeliveryVerificationFlowRoute>(
                        startDestination = ReceiverRoute.IdentityVerificationIntroRoute,
                    ) {
                        composable<ReceiverRoute.IdentityVerificationIntroRoute> { Text("identity intro") }
                        composable<ReceiverRoute.IdentityVerificationEmailRoute> { Text("identity email") }
                        composable<ReceiverRoute.MasterKeyRoute> { Text("master key") }
                        composable<ReceiverRoute.DocumentUploadRoute> { Text("documents") }
                        composable<ReceiverRoute.DeliveryVerificationCompleteRoute> { Text("complete") }
                    }
                }
            }
        }
        composeRule.onNodeWithText("records").assertIsDisplayed()

        val receiverActions = checkNotNull(actions)
        composeRule.runOnIdle { receiverActions.navigateToSenderDetail("sender-7") }
        composeRule.onNodeWithText("sender detail").assertIsDisplayed()
        composeRule.runOnIdle { receiverActions.navigateToDeliveryVerificationFlow("sender-7") }
        composeRule.onNodeWithText("identity intro").assertIsDisplayed()
        composeRule.runOnIdle { receiverActions.navigateToIdentityVerificationEmail() }
        composeRule.onNodeWithText("identity email").assertIsDisplayed()
        composeRule.runOnIdle { receiverActions.proceedToMasterKey() }
        composeRule.onNodeWithText("master key").assertIsDisplayed()
        composeRule.runOnIdle { receiverActions.popBack() }
        composeRule.onNodeWithText("sender detail").assertIsDisplayed()
        composeRule.onNodeWithText("identity email").assertDoesNotExist()

        composeRule.runOnIdle { receiverActions.navigateToDeliveryVerificationFlow("sender-7") }
        composeRule.runOnIdle { receiverActions.navigateToIdentityVerificationEmail() }
        composeRule.runOnIdle { receiverActions.proceedToMasterKey() }
        composeRule.runOnIdle { receiverActions.proceedToDocumentUpload() }
        composeRule.onNodeWithText("documents").assertIsDisplayed()
        composeRule.runOnIdle { receiverActions.proceedToDeliveryVerificationComplete() }
        composeRule.onNodeWithText("complete").assertIsDisplayed()
        composeRule.runOnIdle { receiverActions.popToReceivedRecords() }

        composeRule.onNodeWithText("records").assertIsDisplayed()
        composeRule.onNodeWithText("complete").assertDoesNotExist()
        composeRule.onNodeWithText("sender detail").assertDoesNotExist()
    }

    @Test
    fun homeActions_routeImplementedEntryPointsToExactDestinations() {
        var actions: HomeTabActions? = null
        var appState: AppState? = null
        composeRule.setContent {
            val currentAppState = rememberAfternoteAppState()
            val homeActions = rememberHomeTabActions(currentAppState, onRetryLoad = {})
            SideEffect {
                appState = currentAppState
                actions = homeActions
            }
            AfternoteTheme {
                NavHost(
                    navController = currentAppState.navController,
                    startDestination = Route.Home,
                ) {
                    composable<Route.Home> { Text("home route") }
                    composable<Route.Afternote> { Text("afternote route") }
                    composable<Route.MindRecord> { Text("mind record route") }
                    composable<Route.TimeLetter> { Text("time letter route") }
                    composable<Route.MemorySpace> { Text("memory space route") }
                    composable<Route.Setting> { Text("setting route") }
                }
            }
        }
        composeRule.onNodeWithText("home route").assertIsDisplayed()

        val homeActions = checkNotNull(actions)
        composeRule.runOnIdle { homeActions.onNextStepClick() }
        composeRule.onNodeWithText("afternote route").assertIsDisplayed()
        // 카테고리 카드는 시안에 없어 사라졌지만(#700) 주간 이미지·카운트는 여전히
        // Route.MindRecord 로 간다 — 살아 있는 진입점이라 가드를 그쪽으로 옮긴다 (리뷰 지적).
        composeRule.runOnIdle { homeActions.onWeeklyImageClick() }
        composeRule.onNodeWithText("mind record route").assertIsDisplayed()
        composeRule.runOnIdle { checkNotNull(appState).navController.popBackStack() }
        composeRule.runOnIdle { homeActions.onWeeklyCountClick() }
        composeRule.onNodeWithText("mind record route").assertIsDisplayed()
        composeRule.runOnIdle { checkNotNull(appState).navController.popBackStack() }

        // 2026-08-09 확정된 타임레터 NEXT STEP 카드의 목적지 (#700).
        composeRule.runOnIdle { homeActions.onTimeLetterNextStepClick() }
        composeRule.onNodeWithText("time letter route").assertIsDisplayed()

        composeRule.runOnIdle { checkNotNull(appState).navController.popBackStack() }
        composeRule.runOnIdle { homeActions.onMemoriesSectionClick() }
        composeRule.onNodeWithText("memory space route").assertIsDisplayed()
        composeRule.runOnIdle { homeActions.onSettingClick() }
        composeRule.onNodeWithText("setting route").assertIsDisplayed()
    }

    @Test
    fun receivedGalleryDetail_routesGalleryContractInsteadOfSocialCredentials() {
        val detailResults = ArrayDeque<Result<ReceivedAfternoteDetail>>()
        val repository =
            FakeReceiverRepository.strict().apply {
                onGetReceivedAfternoteDetail = { detailResults.removeFirst() }
            }
        detailResults.addLast(
            Result.success(
                ReceivedAfternoteDetail(
                    serviceName = "Google Drive",
                    senderName = "이발신",
                    createdAt = "2026.08.22",
                    type = AfternoteType.GALLERY_AND_FILES,
                    processingMethods = listOf("가족에게 폴더 전달"),
                    leaveMessageBlocks =
                        listOf(LeaveMessageBlock(title = "사진", body = "여행 사진을 보관해 줘")),
                ),
            ),
        )
        val viewModel =
            ReceivedAfternoteDetailViewModel(
                savedStateHandle = SavedStateHandle(mapOf("afternoteId" to 202L)),
                receiverRepository = repository,
                errorReporter = FakeErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                ReceivedAfternoteDetailRoute(
                    onNavigateBack = {},
                    onNavigateToFullList = {},
                    onNavigateToPlaylist = {},
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("Google Drive").assertIsDisplayed()
        composeRule.onNodeWithText("가족에게 폴더 전달").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("여행 사진을 보관해 줘").performScrollTo().assertIsDisplayed()
        assertEquals(listOf(202L), repository.requestedDetailIds)
    }

    @Test
    fun receivedMemorialDetail_routesPlaylistContractAndForwardsExactId() {
        val detailResults = ArrayDeque<Result<ReceivedAfternoteDetail>>()
        val repository =
            FakeReceiverRepository.strict().apply {
                onGetReceivedAfternoteDetail = { detailResults.removeFirst() }
            }
        detailResults.addLast(
            Result.success(
                ReceivedAfternoteDetail(
                    serviceName = "추억 노트",
                    senderName = "이발신",
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
                savedStateHandle = SavedStateHandle(mapOf("afternoteId" to 303L)),
                receiverRepository = repository,
                errorReporter = FakeErrorReporter(),
            )
        val playlistRoutes = mutableListOf<Long>()

        composeRule.setContent {
            AfternoteTheme {
                ReceivedAfternoteDetailRoute(
                    onNavigateBack = {},
                    onNavigateToFullList = {},
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

        assertEquals(listOf(303L), playlistRoutes)
        assertEquals(listOf(303L), repository.requestedDetailIds)
    }
}

private data class PendingHomeAttempt(
    val afterNotes: CompletableDeferred<Result<AfterNotesListResult>>,
    val mindRecords: CompletableDeferred<Result<ReceiverMindRecords>>,
    val timeLetters: CompletableDeferred<Result<ReceivedTimeLetterList>>,
    val senderMessage: CompletableDeferred<Result<SenderMessageInfo?>>,
) {
    fun complete(
        afterNotes: Result<AfterNotesListResult>,
        mindRecords: Result<ReceiverMindRecords>,
        timeLetters: Result<ReceivedTimeLetterList>,
        senderMessage: Result<SenderMessageInfo?>,
    ) {
        this.afterNotes.complete(afterNotes)
        this.mindRecords.complete(mindRecords)
        this.timeLetters.complete(timeLetters)
        this.senderMessage.complete(senderMessage)
    }
}

/** 홈 한 번의 로드가 물리는 세 리포지토리 대기열에 결과 게이트를 한 벌씩 건다. */
private fun enqueueHomeAttempt(
    afterNoteHomeResults: ArrayDeque<CompletableDeferred<Result<AfterNotesListResult>>>,
    mindRecordHomeResults: ArrayDeque<CompletableDeferred<Result<ReceiverMindRecords>>>,
    timeLetterHomeResults: ArrayDeque<CompletableDeferred<Result<ReceivedTimeLetterList>>>,
    senderMessageHomeResults: ArrayDeque<CompletableDeferred<Result<SenderMessageInfo?>>>,
): PendingHomeAttempt {
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

/**
 * ServerRejection 이 나르는 원인 예외 자리. 프로덕션에서는 `ApiException` 이 들어오지만, 도메인 계약이
 * 요구하는 것은 `Throwable` 뿐이라 이 테스트들은 core:network 를 끌어오지 않는다.
 */
private val CAUSE: Throwable = IOException("stub cause")
