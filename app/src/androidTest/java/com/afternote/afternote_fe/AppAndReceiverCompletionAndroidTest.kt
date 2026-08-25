package com.afternote.afternote_fe

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.paging.PagingData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.navigation.rememberAfternoteAppState
import com.afternote.afternote_fe.navigation.rememberHomeTabActions
import com.afternote.afternote_fe.navigation.rememberReceiverNavActions
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeAuthRepository
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.UserProfileRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.model.Session
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.home.presentation.HomeTabActions
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.model.AfterNotesListResult
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistDetail
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistSong
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentSlot
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadScreen
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationViewModel
import com.afternote.feature.receiver.presentation.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.receiver.presentation.detail.ReceivedAfternoteDetailViewModel
import com.afternote.feature.receiver.presentation.home.ReceiverHomeActions
import com.afternote.feature.receiver.presentation.home.ReceiverHomeEvent
import com.afternote.feature.receiver.presentation.home.ReceiverHomeScreen
import com.afternote.feature.receiver.presentation.home.ReceiverHomeViewModel
import com.afternote.feature.receiver.presentation.home.model.ReceiverHomeUiState
import com.afternote.feature.receiver.presentation.navigation.ReceiverNavActions
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.afternote.presentation.R as AfternoteFeatureR
import com.afternote.feature.home.presentation.R as HomeR
import com.afternote.feature.onboarding.presentation.R as OnboardingR
import com.afternote.feature.receiver.presentation.R as ReceiverR

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AppAndReceiverCompletionAndroidTest {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

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
            Result.failure(CoreAuthFailure.InvalidLoginCredentials(IllegalStateException("rejected"))),
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
            context.getString(AfternoteFeatureR.string.feature_afternote_fingerprint_login_title)
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
        assertTrue(fakeErrorReporter.failures.isEmpty())
    }

    @Test
    fun welcomeCheckRecords_opensActualReceivedRecordsStartDestination() {
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.welcome_check_records))
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_records_box_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(AfternoteFeatureR.string.receiver_records_box_empty))
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
        val repository = CompletionReceiverRepository()
        // 완료 시점을 테스트가 쥐어야 세 조회의 경합 순서를 만들 수 있다.
        val mindRecordHomeResults = ArrayDeque<CompletableDeferred<Result<ReceiverMindRecords>>>()
        val mindRecordRepository =
            FakeMindRecordReceiverRepository(onGetAll = { mindRecordHomeResults.removeFirst().await() })
        val timeLetterRepository = CompletionReceiverTimeLetterRepository()

        fun homeCallCounts(): List<Int> =
            listOf(
                repository.afterNotesCalls,
                mindRecordRepository.getAllCalls,
                timeLetterRepository.listCalls,
                repository.senderMessageCalls,
            )

        val allFailureAttempt = enqueueHomeAttempt(repository, mindRecordHomeResults, timeLetterRepository)
        val partialAttempt = enqueueHomeAttempt(repository, mindRecordHomeResults, timeLetterRepository)
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
                    actions = ReceiverHomeActions.Noop,
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
            .onNodeWithText(context.getString(ReceiverR.string.receiver_home_error_message))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_home_retry))
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
            .onNodeWithText(context.getString(ReceiverR.string.receiver_home_sender_record_title, "이발신"))
            .assertIsDisplayed()
        composeRule.onNodeWithText("언제나 응원할게").assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(ReceiverR.string.receiver_home_section_count_unavailable))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("8개 라이프 이벤트 레터가 있습니다.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("2개의 애프터노트가 있습니다.")
            .performScrollTo()
            .assertIsDisplayed()

        assertEquals(2, reporter.failures.size)
        assertEquals("receiver_home_load", reporter.failures[0].second["home_stage"])
        assertEquals("receiver_home_partial_load", reporter.failures[1].second["home_stage"])
        assertEquals("mind_records", reporter.failures[1].second["home_failed_sources"])
        assertEquals(listOf(2, 2, 2, 2), homeCallCounts())
    }

    @Test
    fun emailCodeExpired_resendAndNewCode_verifyExactlyOnce() {
        val authRepository = CompletionReceiverAuthRepository()
        authRepository.verifyEmailResults.addLast(
            Result.failure(
                ReceiverFailure.ServerRejection(
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
    fun documentSlot_replaceFailureKeepsPreviousThenSuccessReflectsReplacement() {
        val uploadRepository = CompletionDocumentUploadRepository()
        uploadRepository.results.addLast(Result.success("https://cdn.example.test/original.pdf"))
        uploadRepository.results.addLast(Result.failure(IllegalStateException("replacement failed")))
        uploadRepository.results.addLast(Result.success("https://cdn.example.test/replacement.pdf"))
        val viewModel =
            DocumentUploadViewModel(
                uploadRepository,
                CompletionReceiverAuthRepository(),
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
        assertEquals(3, uploadRepository.calls)
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
                    composable<Route.MemorySpace> { Text("memory space route") }
                    composable<Route.Setting> { Text("setting route") }
                }
            }
        }
        composeRule.onNodeWithText("home route").assertIsDisplayed()

        val homeActions = checkNotNull(actions)
        composeRule.runOnIdle { homeActions.onNextStepClick() }
        composeRule.onNodeWithText("afternote route").assertIsDisplayed()
        composeRule.runOnIdle { homeActions.onRecordCategoryClick(MindRecordCategory.DIARY) }
        composeRule.onNodeWithText("mind record route").assertIsDisplayed()

        composeRule.runOnIdle { checkNotNull(appState).navController.popBackStack() }
        composeRule.runOnIdle { homeActions.onMemoriesSectionClick() }
        composeRule.onNodeWithText("memory space route").assertIsDisplayed()
        composeRule.runOnIdle { homeActions.onSettingClick() }
        composeRule.onNodeWithText("setting route").assertIsDisplayed()
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
                savedStateHandle = SavedStateHandle(mapOf("afternoteId" to 202L)),
                receiverRepository = repository,
                errorReporter = FakeErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                ReceivedAfternoteDetailRoute(onBack = {}, onNavigateToPlaylist = {}, viewModel = viewModel)
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
                savedStateHandle = SavedStateHandle(mapOf("afternoteId" to 303L)),
                receiverRepository = repository,
                errorReporter = FakeErrorReporter(),
            )
        val playlistRoutes = mutableListOf<Long>()

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

        assertEquals(listOf(303L), playlistRoutes)
        assertEquals(listOf(303L), repository.detailIds)
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
    receiverRepository: CompletionReceiverRepository,
    mindRecordHomeResults: ArrayDeque<CompletableDeferred<Result<ReceiverMindRecords>>>,
    timeLetterRepository: CompletionReceiverTimeLetterRepository,
): PendingHomeAttempt {
    val attempt =
        PendingHomeAttempt(
            afterNotes = CompletableDeferred(),
            mindRecords = CompletableDeferred(),
            timeLetters = CompletableDeferred(),
            senderMessage = CompletableDeferred(),
        )
    receiverRepository.afterNoteHomeResults.addLast(attempt.afterNotes)
    mindRecordHomeResults.addLast(attempt.mindRecords)
    timeLetterRepository.homeResults.addLast(attempt.timeLetters)
    receiverRepository.senderMessageHomeResults.addLast(attempt.senderMessage)
    return attempt
}

private class CompletionReceiverRepository : ReceiverRepository {
    private val authCode = MutableStateFlow<String?>(null)
    override val authCodeFlow: Flow<String?> = authCode

    val afterNoteHomeResults = ArrayDeque<CompletableDeferred<Result<AfterNotesListResult>>>()
    val senderMessageHomeResults = ArrayDeque<CompletableDeferred<Result<SenderMessageInfo?>>>()

    val detailResults = ArrayDeque<Result<ReceivedAfternoteDetail>>()
    val detailIds = mutableListOf<Long>()
    var afterNotesCalls = 0
        private set
    var senderMessageCalls = 0
        private set

    override suspend fun currentAuthCode(): String? = authCode.value

    override suspend fun saveAuthCode(code: String) {
        authCode.value = code
    }

    override fun getPagedReceivedAfternotes(): Flow<PagingData<AfterNoteListItem>> = flowOf(PagingData.empty())

    override suspend fun getReceivedAfterNotes(): Result<AfterNotesListResult> {
        afterNotesCalls += 1
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

    override suspend fun loadSenderMessage(): Result<SenderMessageInfo?> {
        senderMessageCalls += 1
        return senderMessageHomeResults.removeFirst().await()
    }
}

private class CompletionReceiverTimeLetterRepository : ReceiverTimeLetterRepository {
    val homeResults = ArrayDeque<CompletableDeferred<Result<ReceivedTimeLetterList>>>()
    var listCalls = 0
        private set

    // 실패는 throw 로 전달 — 인터페이스가 Result 대신 예외 계약이라 ViewModel 쪽 runCatching 이 받는다.
    override suspend fun getReceivedTimeLetters(): ReceivedTimeLetterList {
        listCalls += 1
        return homeResults.removeFirst().await().getOrThrow()
    }

    override suspend fun getReceivedTimeLetterDetail(timeLetterReceiverId: Long): ReceivedTimeLetter {
        error("unexpected getReceivedTimeLetterDetail")
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
    val results = ArrayDeque<Result<String>>()
    var calls = 0

    override suspend fun upload(
        bytes: ByteArray,
        extension: String,
    ): Result<String> {
        calls += 1
        return results.removeFirstOrNull()
            ?: Result.success("https://cdn.example.test/death.pdf")
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
