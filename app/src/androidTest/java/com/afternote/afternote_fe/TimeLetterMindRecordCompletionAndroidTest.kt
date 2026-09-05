package com.afternote.afternote_fe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.afternote_fe.test.testReceiver
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.model.WeeklyReportEmotion
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
import com.afternote.feature.mindrecord.presentation.screen.memoryspace.MemorySpaceScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionAnswerListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DraftListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.WeeklyReportScreen
import com.afternote.feature.mindrecord.presentation.usecase.DeleteMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.MemorySpaceViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportViewModel
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.testing.FakeFileMetadataRepository
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeVoiceRecorderRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.domain.usecase.ResolveTimeLetterBlocksUseCase
import com.afternote.feature.timeletter.presentation.screen.sender.RecipientListScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterDetailScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientListViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterDetailUiState
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterDetailViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteError
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

/** #838/#839에서 기존 production Screen/ViewModel로 표현 가능한 미검증 완료 경계. */
@RunWith(AndroidJUnit4::class)
class TimeLetterMindRecordCompletionAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun timeLetterWrite_uiValidationAndRapidRegister_preserveInputAndCreateOnce() {
        val allowCreate = CompletableDeferred<Unit>()
        val repository =
            FakeTimeLetterRepository.strict().apply {
                onGetTimeLetters = { registeredLetters }
                onGetTemporaryTimeLetters = { temporaryLetters }
                onCreateTimeLetter = { call ->
                    allowCreate.await()
                    completionCreatedLetter(call)
                }
            }
        val userRepository = appTestUserRepository(receivers = listOf(testReceiver()))
        val viewModel = timeLetterWriteViewModel(repository, userRepository)

        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = uiState,
                    onRegisterClick = viewModel::register,
                    onTitleChanged = viewModel::updateDraftTitle,
                    onTextContentChanged = viewModel::updateDraftTextContent,
                    onErrorShown = {},
                    onAddLinkBlock = viewModel::addLinkBlock,
                    onSetFocusedBlock = viewModel::setFocusedBlock,
                )
            }
        }

        val register = composeRule.onNode(hasText("등록") and hasClickAction())
        register.performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.error == TimeLetterWriteError.RecipientRequired
        }
        composeRule.onNodeWithText("수신자를 선택해주세요.").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = TIMEOUT) { userRepository.getReceiversCalls == 1 }
        composeRule.runOnIdle {
            viewModel.clearError()
            viewModel.setRecipients(listOf(7L))
        }
        register.performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.error == TimeLetterWriteError.SendDateRequired
        }
        composeRule.onNodeWithText("발송 날짜를 선택해주세요.").assertIsDisplayed()

        composeRule.runOnIdle {
            viewModel.clearError()
            viewModel.setSendAt("2026-09-14")
            viewModel.setSendTime(hour = 9, minute = 35)
            viewModel.addLinkBlock("https://afternote.test/remember")
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.editorBlocks.size == 3
        }
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(3)
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("오래 보존할 제목")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("첫 번째 실제 텍스트 블록")
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.draftTitle == "오래 보존할 제목" &&
                viewModel.uiState.value.draftTextContents[0L] == "첫 번째 실제 텍스트 블록"
        }

        // 한 입력 이벤트 안에 두 번 tap해 recomposition 전 연타도 ViewModel guard가 막는지 본다.
        register.performTouchInput {
            click()
            click()
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            repository.createCalls.size == 1 && viewModel.uiState.value.isSaving
        }

        val inFlight = viewModel.uiState.value
        assertEquals(listOf(7L), inFlight.recipientIds)
        assertEquals(listOf("김수신"), inFlight.recipientNames)
        assertEquals("오래 보존할 제목", inFlight.draftTitle)
        assertEquals("첫 번째 실제 텍스트 블록", inFlight.draftTextContents[0L])
        composeRule.onNodeWithText("오래 보존할 제목").assertIsDisplayed()
        composeRule.onNodeWithText("첫 번째 실제 텍스트 블록").assertIsDisplayed()
        composeRule.onNodeWithText("김수신 님에게").assertIsDisplayed()

        val call = repository.createCalls.single()
        assertEquals("오래 보존할 제목", call.title)
        assertEquals(listOf(7L), call.receiverIds)
        assertEquals("2026-09-14T09:35:00", call.sendAt)
        assertEquals(TimeLetterDeliveryMode.DATE, call.deliveryMode)
        assertEquals(TimeLetterStatus.SCHEDULED, call.status)
        assertEquals(
            listOf(TimeLetterBlockType.TEXT, TimeLetterBlockType.LINK),
            call.blocks.map(NewTimeLetterBlock::blockType),
        )
        assertEquals(listOf(1, 2), call.blocks.map(NewTimeLetterBlock::blockOrder))
        assertEquals("첫 번째 실제 텍스트 블록", call.blocks[0].textContent)
        assertEquals("https://afternote.test/remember", call.blocks[1].url)

        composeRule.runOnIdle { allowCreate.complete(Unit) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.registered && !viewModel.uiState.value.isSaving
        }
        assertEquals(1, repository.getTimeLettersCalls)
        assertEquals(1, repository.createCalls.size)
    }

    @Test
    fun timeLetterRecipientSelector_roundTripPreservesTitleTextAndExactReceiverId() {
        val repository =
            FakeTimeLetterRepository.strict().apply {
                onGetTemporaryTimeLetters = { temporaryLetters }
            }
        val userRepository = appTestUserRepository(receivers = listOf(testReceiver()))
        val writeViewModel = timeLetterWriteViewModel(repository, userRepository)
        val recipientViewModel = RecipientListViewModel(userRepository)
        var showRecipient by mutableStateOf(false)

        composeRule.setContent {
            AfternoteTheme {
                if (showRecipient) {
                    RecipientListScreen(
                        onBackClick = { showRecipient = false },
                        onConfirmClick = { recipients ->
                            writeViewModel.setRecipients(recipients.map { it.receiverId })
                            showRecipient = false
                        },
                        viewModel = recipientViewModel,
                    )
                } else {
                    val uiState by writeViewModel.uiState.collectAsStateWithLifecycle()
                    TimeLetterWriteScreen(
                        uiState = uiState,
                        onRecipientClick = { title, textContents ->
                            writeViewModel.updateDraftContent(title, textContents)
                            showRecipient = true
                        },
                        onTitleChanged = writeViewModel::updateDraftTitle,
                        onTextContentChanged = writeViewModel::updateDraftTextContent,
                    )
                }
            }
        }

        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("왕복 보존 제목")
        composeRule.onAllNodes(hasSetTextAction())[1].performTextInput("왕복 보존 본문")
        composeRule.onNodeWithText("수신자를 선택해주세요").performClick()
        composeRule.onNodeWithText("수신인 목록").assertIsDisplayed()
        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onAllNodes(checkboxMatcher)[0].performClick()
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            !showRecipient && writeViewModel.uiState.value.recipientIds == listOf(7L)
        }
        val restored = writeViewModel.uiState.value
        assertEquals("왕복 보존 제목", restored.draftTitle)
        assertEquals("왕복 보존 본문", restored.draftTextContents[0L])
        assertEquals(listOf(7L), restored.recipientIds)
        assertEquals(listOf("김수신"), restored.recipientNames)
        composeRule.onNodeWithText("왕복 보존 제목").assertIsDisplayed()
        composeRule.onNodeWithText("왕복 보존 본문").assertIsDisplayed()
        composeRule.onNodeWithText("김수신 님에게").assertIsDisplayed()
        assertTrue(repository.createCalls.isEmpty())
    }

    @Test
    fun timeLetterSenderDetail_routeIdLoadingFailureRetryAndSuccess_areConnected() {
        val firstLoad = CompletableDeferred<Result<TimeLetter>>()
        val detailLetter = completionDetailLetter()
        var nextDetailGate: CompletableDeferred<Result<TimeLetter>>? = firstLoad
        val detailResults = ArrayDeque<Result<TimeLetter>>()
        detailResults.addLast(Result.success(completionDetailLetter()))
        val repository =
            FakeTimeLetterRepository.strict().apply {
                registeredLetters = TimeLetterList(listOf(detailLetter), 1)
                onGetTimeLetters = { registeredLetters }
                onGetTimeLetter = {
                    val gate = nextDetailGate
                    nextDetailGate = null
                    (gate?.await() ?: requireNotNull(detailResults.removeFirstOrNull())).getOrThrow()
                }
            }
        val userRepository = appTestUserRepository(receivers = listOf(testReceiver()))
        val listViewModel = TimeletterViewModel(repository, userRepository)
        var detailViewModel by mutableStateOf<TimeLetterDetailViewModel?>(null)

        composeRule.setContent {
            val activeDetailViewModel = detailViewModel
            AfternoteTheme {
                if (activeDetailViewModel == null) {
                    TimeletterScreen(
                        viewModel = listViewModel,
                        onLetterClick = { timeLetterId ->
                            detailViewModel =
                                TimeLetterDetailViewModel(
                                    timeLetterRepository = repository,
                                    userRepository = userRepository,
                                    savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to timeLetterId)),
                                )
                        },
                    )
                } else {
                    TimeLetterDetailScreen(onBackClick = {}, viewModel = activeDetailViewModel)
                }
            }
        }

        composeRule.onNode(hasText("상세 route 편지") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { repository.requestedDetailIds == listOf(509L) }
        assertEquals(TimeLetterDetailUiState.Loading, detailViewModel?.uiState?.value)
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()

        composeRule.runOnIdle {
            firstLoad.complete(Result.failure(IllegalStateException("sender detail offline")))
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            detailViewModel?.uiState?.value == TimeLetterDetailUiState.Error
        }
        composeRule.onNodeWithText("타임레터를 불러올 수 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            detailViewModel?.uiState?.value is TimeLetterDetailUiState.Success
        }
        composeRule.onNodeWithText("상세 route 편지").assertIsDisplayed()
        composeRule.onNodeWithText("수신인  김수신").assertIsDisplayed()
        composeRule.onNodeWithText("발송 예정일  2026.10.09.").assertIsDisplayed()
        composeRule.onNodeWithText("상세에서 복구된 본문").assertIsDisplayed()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        composeRule
            .onNodeWithText("🔗 https://afternote.test/detail")
            .assertIsDisplayed()
        assertEquals(listOf(509L, 509L), repository.requestedDetailIds)
        assertEquals(1, repository.getTimeLettersCalls)
        assertEquals(3, userRepository.getReceiversCalls)
    }

    @Test
    fun mindRecordHome_dailyQuestionLoadingEmptyAndErrorRetrySuccess_areRendered() {
        val emptyGate = CompletableDeferred<Result<List<DailyQuestion>>>()
        val emptyRepository =
            FakeDailyQuestionRepository(today = completionToday()).apply {
                // 첫 조회를 붙잡아 로딩 상태를 만든다. 그 뒤 조회는 저장소 기본 동작(0건).
                var gate: CompletableDeferred<Result<List<DailyQuestion>>>? = emptyGate
                onGetList = { _, _ ->
                    val pending = gate
                    gate = null
                    pending?.await() ?: Result.success(emptyList())
                }
            }
        var activeViewModel by mutableStateOf(DailyQuestionListViewModel(emptyRepository, MindRecordChangeTracker(), FakeErrorReporter()))

        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionAnswerListScreen(
                    viewModel = activeViewModel,
                    onItemClick = { _, _ -> },
                    onEditClick = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) { emptyRepository.listQueries.size == 1 }
        assertEquals(DailyQuestionListUiState.Loading, activeViewModel.uiState.value)
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()

        composeRule.runOnIdle { emptyGate.complete(Result.success(emptyList())) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (activeViewModel.uiState.value as? DailyQuestionListUiState.Success)?.answers?.isEmpty() == true
        }
        composeRule.onNodeWithText("아직 등록된 답변이 없어요.").assertIsDisplayed()

        val listResults =
            ArrayDeque<Result<List<DailyQuestion>>>().apply {
                addLast(Result.failure(IllegalStateException("home offline")))
            }
        val retryRepository =
            FakeDailyQuestionRepository(today = completionToday()).apply {
                onGetList = { _, _ -> listResults.removeFirst() }
            }
        val retryViewModel = DailyQuestionListViewModel(retryRepository, MindRecordChangeTracker(), FakeErrorReporter())
        composeRule.runOnIdle { activeViewModel = retryViewModel }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            retryViewModel.uiState.value is DailyQuestionListUiState.Error
        }
        composeRule.onNodeWithText("데일리 질문을 불러오지 못했습니다.").assertIsDisplayed()

        listResults.addLast(
            Result.success(
                listOf(
                    dailyQuestion(id = 81L, title = "재시도로 돌아온 답변"),
                    dailyQuestion(id = 82L, title = "노출되면 안 되는 임시답변", isDraft = true),
                ),
            ),
        )
        composeRule.runOnIdle { retryViewModel.refreshOnReturn() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (retryViewModel.uiState.value as? DailyQuestionListUiState.Success)?.answers?.size == 1
        }
        composeRule.onNodeWithText("재시도로 돌아온 답변").assertIsDisplayed()
        composeRule.onNodeWithText("노출되면 안 되는 임시답변").assertDoesNotExist()
        assertEquals(2, retryRepository.listQueries.size)
        assertEquals(2, retryRepository.getTodayCalls)
    }

    @Test
    fun dailyQuestionWrite_successRefreshesExistingListWithCreatedAnswer() {
        // 프로덕션에서는 data 계층이 쓰기 성공에 notifyChanged() 를 부르고, 목록이 그 버전을
        // 보고 재조회한다 (#736). fake 가 그 배선을 흉내 내지 않으면 «작성하고 돌아왔는데
        // 목록이 그대로»(#520) 를 잡는 이 테스트가 조용히 침묵한다 (#966 리뷰).
        val changeTracker = MindRecordChangeTracker()
        val repository = FakeDailyQuestionRepository(today = completionToday(), changeTracker = changeTracker)
        val listViewModel = DailyQuestionListViewModel(repository, changeTracker, FakeErrorReporter())
        var writeViewModel by mutableStateOf<DailyQuestionWriteViewModel?>(null)
        var submitSuccessCalls = 0

        composeRule.setContent {
            val activeWriteViewModel = writeViewModel
            AfternoteTheme {
                if (activeWriteViewModel == null) {
                    DailyQuestionAnswerListScreen(
                        viewModel = listViewModel,
                        onItemClick = { _, _ -> },
                        onEditClick = {},
                    )
                } else {
                    DailyQuestionWriteScreen(
                        viewModel = activeWriteViewModel,
                        onSubmitSuccess = {
                            submitSuccessCalls += 1
                            writeViewModel = null
                            listViewModel.refreshOnReturn()
                        },
                        onBackClick = {},
                        onDraftListClick = {},
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (listViewModel.uiState.value as? DailyQuestionListUiState.Success)?.answers?.isEmpty() == true
        }
        composeRule.onNodeWithText("아직 등록된 답변이 없어요.").assertIsDisplayed()
        val initialListQueryCount = repository.listQueries.size
        val initialGetTodayCalls = repository.getTodayCalls

        composeRule.runOnIdle {
            writeViewModel =
                DailyQuestionWriteViewModel(
                    savedStateHandle = SavedStateHandle(emptyMap()),
                    repository = repository,
                    photoUploadRepository = FakePhotoUploadRepository.strict(),
                    draftLoader = LoadMindRecordDraftsUseCase(FakeDiaryRepository(), repository),
                    errorReporter = FakeErrorReporter(),
                )
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            writeViewModel?.uiState?.value?.isQuestionLoading == false
        }
        composeRule.runOnIdle {
            requireNotNull(writeViewModel).onAnswerChanged("작성 후 목록에 반영될 답변")
        }
        composeRule.onNodeWithText("오늘의 테스트 질문").assertIsDisplayed()
        composeRule.onNode(hasText("저장") and hasClickAction()).performClick()

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            submitSuccessCalls == 1 &&
                (listViewModel.uiState.value as? DailyQuestionListUiState.Success)?.answers?.size == 1
        }
        composeRule.onAllNodes(hasText("오늘의 테스트 질문"))[0].assertIsDisplayed()
        composeRule.onNodeWithText("작성 후 목록에 반영될 답변").assertIsDisplayed()
        assertEquals(1, repository.createdPayloads.size)
        assertEquals(71L, repository.createdPayloads.single().questionId)
        assertEquals(false, repository.createdPayloads.single().isDraft)
        assertEquals(initialListQueryCount + 2, repository.listQueries.size)
        assertEquals(
            listOf(
                FakeDailyQuestionRepository.ListQuery(date = null, draftOnly = true),
                FakeDailyQuestionRepository.ListQuery(date = null, draftOnly = null),
            ),
            repository.listQueries.drop(initialListQueryCount),
        )
        assertEquals(initialGetTodayCalls + 2, repository.getTodayCalls)
    }

    @Test
    fun diaryDraftRow_routesIdAndMonthThenPrefillsAndPublishesWithPatch() {
        val currentMonth = YearMonth.now()
        val draftDate = currentMonth.atDay(12)
        val repository =
            FakeDiaryRepository(
                initialDiaries =
                    listOf(
                        Diary(
                            diaryId = 612L,
                            title = "이어 쓸 일기",
                            content = "<p>임시 본문</p>",
                            date = draftDate.toString(),
                            createdAt = draftDate.toString(),
                            todayMood = TodayMood.SOSO,
                            imageUrl = "https://afternote.test/draft.jpg",
                            isDraft = true,
                        ),
                    ),
            )
        val draftDailyQuestionRepository = FakeDailyQuestionRepository(today = completionToday())
        val draftListViewModel =
            DraftListViewModel(
                loadDrafts = LoadMindRecordDraftsUseCase(repository, draftDailyQuestionRepository),
                deleteDrafts = DeleteMindRecordDraftsUseCase(repository, draftDailyQuestionRepository),
                errorReporter = FakeErrorReporter(),
            )
        var routedArguments: Pair<Long, String>? = null
        var writeViewModel by mutableStateOf<DiaryWriteViewModel?>(null)
        var submitSuccessCalls = 0

        composeRule.setContent {
            val activeWriteViewModel = writeViewModel
            AfternoteTheme {
                if (activeWriteViewModel == null) {
                    DraftListScreen(
                        viewModel = draftListViewModel,
                        onDiaryDraftClick = { draftId, draftYearMonth ->
                            routedArguments = draftId to draftYearMonth
                            writeViewModel =
                                DiaryWriteViewModel(
                                    savedStateHandle =
                                        SavedStateHandle(
                                            mapOf(
                                                "recordId" to draftId,
                                                "yearMonth" to draftYearMonth,
                                                "isDraft" to true,
                                            ),
                                        ),
                                    repository = repository,
                                    photoUploadRepository = FakePhotoUploadRepository.strict(),
                                    userRepository = appTestUserRepository(),
                                    draftLoader =
                                        LoadMindRecordDraftsUseCase(repository, draftDailyQuestionRepository),
                                    errorReporter = FakeErrorReporter(),
                                )
                        },
                        onBackClick = {},
                        onDailyQuestionDraftClick = {},
                    )
                } else {
                    DiaryWriteScreen(
                        viewModel = activeWriteViewModel,
                        onSubmitSuccess = { submitSuccessCalls += 1 },
                        onBackClick = {},
                        onDraftListClick = {},
                    )
                }
            }
        }

        composeRule.onNode(hasText("이어 쓸 일기") and hasClickAction()).assertIsDisplayed()
        val initialDiaryQueryCount = repository.listQueries.size
        composeRule.onNode(hasText("이어 쓸 일기") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            writeViewModel?.uiState?.value?.draftLoaded == true
        }
        assertEquals(612L to currentMonth.toString(), routedArguments)
        val loaded = requireNotNull(writeViewModel).uiState.value
        assertEquals("이어 쓸 일기", loaded.title)
        assertEquals("<p>임시 본문</p>", loaded.content)
        assertEquals(TodayMood.SOSO, loaded.mood)
        assertEquals(draftDate, loaded.date)
        // 프리필은 «대표 이미지» 를 상태로 들지 않는다 — 읽는 곳이 없고 계약에도 없다 (#1195).
        // 본문 이미지는 content 의 img 태그로 이어진다.
        composeRule.onNodeWithText("이어 쓸 일기").assertIsDisplayed()

        composeRule.runOnIdle {
            requireNotNull(writeViewModel).onTitleChanged("완성한 일기")
            requireNotNull(writeViewModel).onContentChanged("<p>완성한 본문</p>")
        }
        composeRule.onNode(hasText("등록") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { submitSuccessCalls == 1 }

        val update = repository.updatedPayloads.single()
        assertEquals(612L, update.first)
        assertEquals("완성한 일기", update.second.title)
        assertEquals("<p>완성한 본문</p>", update.second.content)
        assertEquals(false, update.second.isDraft)
        assertEquals(TodayMood.SOSO, update.second.todayMood)
        // date·imageUrl 은 수정 요청 계약에 없어 페이로드에서 걷었다 (#955).
        assertTrue(repository.createdPayloads.isEmpty())
        assertEquals(initialDiaryQueryCount + 2, repository.listQueries.size)
        assertTrue(
            repository.listQueries.drop(initialDiaryQueryCount).all {
                it == FakeDiaryRepository.ListQuery(currentMonth.toString(), true)
            },
        )
    }

    @Test
    fun memorySpace_supportedSuccess_opensAndClosesDetailThenNavigatesBack() {
        val memoryDate = LocalDate.now()
        val memory =
            Diary(
                diaryId = 501L,
                title = "추억이 된 하루",
                content = "이 순간은 나에게 특별한 의미가 있었습니다.",
                date = memoryDate.toString(),
                createdAt = memoryDate.toString(),
                todayMood = TodayMood.HAPPY,
                imageUrl = "https://afternote.test/memory.jpg",
                isDraft = false,
            )
        val diaryRepository =
            FakeDiaryRepository(
                onGetList = { yearMonth, _ ->
                    val diaries =
                        if (yearMonth == YearMonth.from(memoryDate).toString()) listOf(memory) else emptyList()
                    Result.success(
                        DiaryList(
                            diaries = diaries,
                            monthDiaryCount = diaries.size,
                            weeklyDominantMood = diaries.firstOrNull()?.todayMood,
                        ),
                    )
                },
            )
        val viewModel = MemorySpaceViewModel(diaryRepository, FakeDailyQuestionRepository(), FakeErrorReporter())
        var backCalls = 0

        composeRule.setContent {
            AfternoteTheme {
                MemorySpaceScreen(
                    viewModel = viewModel,
                    onBackClick = { backCalls += 1 },
                )
            }
        }

        composeRule.onNodeWithText("MEMORY SPACE").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value is MemorySpaceUiState.Success
        }
        composeRule.onNodeWithContentDescription("추억이 된 하루").performClick()
        composeRule
            .onNodeWithText("이 순간은 나에게 특별한 의미가 있었습니다.", substring = true)
            .assertIsDisplayed()
        // 태그는 사용자가 고른 오늘의 기분 이모지다 — 종전 더미의 `#평온` 은 출처가 없었다 (#559).
        composeRule.onNodeWithText("#😊").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("닫기").performClick()
        composeRule
            .onNodeWithText("이 순간은 나에게 특별한 의미가 있었습니다.", substring = true)
            .assertDoesNotExist()

        composeRule.onNodeWithText("돌아가기").performClick()
        composeRule.runOnIdle { assertEquals(1, backCalls) }
    }

    @Test
    fun weeklyReport_errorRetryThenEmptyAndComplete_preservesRequestedWeekContract() {
        val repository =
            FakeWeeklyReportRepository().apply {
                results.addLast(Result.failure(IllegalStateException("weekly offline")))
            }
        val userRepository =
            appTestUserRepository(
                profile = User("주간 사용자", "weekly@afternote.local", null, null),
                receivers = emptyList(),
            )
        val errorReporter = FakeErrorReporter()
        val viewModel =
            WeeklyReportViewModel(
                ObserveWeeklyReportUseCase(repository, userRepository),
                MindRecordChangeTracker(),
                errorReporter,
            )

        composeRule.setContent {
            AfternoteTheme {
                WeeklyReportScreen(viewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value is WeeklyReportUiState.Error
        }
        // 종전에는 여기서 «weekly offline» — 즉 예외 원문 — 이 화면에 뜨는 것을 단언했다.
        // 서버 오류 원문은 화면에 내지 않고 계측으로만 보낸다는 규약(#1339)과 정반대라
        // 결함을 고정하고 있었다. 화면은 안내 문자열로, 원문은 계측으로 확인한다 (#1882).
        val weeklyFailureCopy =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(MindRecordR.string.mindrecord_error_weekly_report_failed)
        composeRule.onNodeWithText(weeklyFailureCopy).assertIsDisplayed()
        composeRule.onNodeWithText("weekly offline").assertDoesNotExist()
        assertEquals(listOf("weekly_report_load"), errorReporter.mindRecordStages)
        val firstMonday = LocalDate.parse(repository.requestedDates.single())

        repository.results.addLast(Result.success(emptyWeeklyReport()))
        composeRule.runOnIdle { viewModel.selectWeek(firstMonday) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (viewModel.uiState.value as? WeeklyReportUiState.Success)?.selectedMonday == firstMonday
        }
        val empty = viewModel.uiState.value as WeeklyReportUiState.Success
        assertEquals(0, empty.recordedDays)
        assertEquals(listOf(0, 0), empty.counts.map { it.first })
        assertTrue(empty.emotionKeywords.isEmpty())
        assertTrue(empty.dailyQuestions.isEmpty())
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        composeRule
            .onNodeWithText("이번 주 주간 사용자 님의 기록에서는 키워드가 나오지 않았어요.")
            .assertIsDisplayed()

        val completeMonday = firstMonday.minusWeeks(1)
        repository.results.addLast(Result.success(completeWeeklyReport(completeMonday)))
        composeRule.runOnIdle { viewModel.selectWeek(completeMonday) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            val state = viewModel.uiState.value as? WeeklyReportUiState.Success
            state?.selectedMonday == completeMonday && state.summaryText == "완전한 주간 요약"
        }
        val complete = viewModel.uiState.value as WeeklyReportUiState.Success
        assertEquals(2, complete.recordedDays)
        assertEquals(listOf(1, 1), complete.counts.map { it.first })
        assertEquals(listOf("감사", "가족"), complete.emotionKeywords.map { it.keyword })
        assertEquals(listOf(75, 25), complete.emotionKeywords.map { it.count })
        assertEquals(1, complete.dailyQuestions.size)
        assertEquals("주간 사용자", complete.userName)

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        composeRule.onNodeWithText("완전한 주간 요약").assertIsDisplayed()
        composeRule.onNodeWithText("감사").assertIsDisplayed()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        composeRule.onNodeWithText("화요일의 질문").assertIsDisplayed()
        assertEquals(
            listOf(firstMonday, firstMonday, completeMonday).map(LocalDate::toString),
            repository.requestedDates,
        )
    }

    private fun timeLetterWriteViewModel(
        repository: FakeTimeLetterRepository,
        userRepository: FakeUserRepository,
    ): TimeLetterWriteViewModel {
        val resolver = ResolveTimeLetterBlocksUseCase(FakePhotoUploadRepository.strict())
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolver),
            resolveTimeLetterBlocksUseCase = resolver,
            timeLetterRepository = repository,
            userRepository = userRepository,
            fileMetadataRepository = FakeFileMetadataRepository.strict(),
            voiceRecorderRepository = FakeVoiceRecorderRepository,
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to null)),
        )
    }

    private companion object {
        const val TIMEOUT = 5_000L

        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}

private fun completionCreatedLetter(call: FakeTimeLetterRepository.CreateCall): TimeLetter =
    TimeLetter(
        id = 901L,
        title = call.title,
        sendAt = call.sendAt,
        deliveredAt = null,
        status = call.status,
        blocks = emptyList(),
        receiverIds = call.receiverIds,
    )

/** 이 파일의 데일리질문 시나리오가 공유하는 "오늘의 질문". `questionId` 를 단언하는 곳이 있다. */
private fun completionToday() = TodayDailyQuestion(71L, 71, "오늘의 테스트 질문", false)

private fun dailyQuestion(
    id: Long,
    title: String,
    isDraft: Boolean = false,
): DailyQuestion =
    DailyQuestion(
        dailyQuestionId = id,
        title = title,
        content = "답변 $id",
        createdAt = "2026-08-20",
        isDraft = isDraft,
    )

private fun completionDetailLetter(): TimeLetter =
    TimeLetter(
        id = 509L,
        title = "상세 route 편지",
        sendAt = "2026-10-09T09:40:00",
        deliveredAt = null,
        status = TimeLetterStatus.SCHEDULED,
        blocks =
            listOf(
                TimeLetterBlock(
                    id = 5091L,
                    blockType = TimeLetterBlockType.TEXT,
                    blockOrder = 1,
                    textContent = "상세에서 복구된 본문",
                    url = null,
                    mimeType = null,
                ),
                TimeLetterBlock(
                    id = 5092L,
                    blockType = TimeLetterBlockType.LINK,
                    blockOrder = 2,
                    textContent = null,
                    url = "https://afternote.test/detail",
                    mimeType = null,
                ),
            ),
        receiverIds = listOf(7L),
    )

private fun emptyWeeklyReport(): WeeklyReport =
    WeeklyReport(
        dailyQuestionAmount = 0,
        diaryAmount = 0,
        summaryText = "",
        week = emptyList(),
        dailyQuestions = emptyList(),
        emotions = emptyList(),
        // 이 테스트들은 분석 상태를 보지 않는다 — 완료로 고정한다 (#725).
        emotionAnalysis = EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0),
    )

private fun completeWeeklyReport(monday: LocalDate): WeeklyReport =
    WeeklyReport(
        dailyQuestionAmount = 1,
        diaryAmount = 1,
        summaryText = "완전한 주간 요약",
        week =
            listOf(
                WeeklyReportDay(
                    diaryId = 91L,
                    day = monday.dayOfMonth,
                    isDiary = true,
                    countsAsRecord = true,
                    emotion = TodayMood.HAPPY,
                ),
            ),
        dailyQuestions =
            listOf(
                WeeklyReportDailyQuestion(
                    title = "화요일의 질문",
                    content = "화요일의 답변",
                    date = monday.plusDays(1),
                ),
            ),
        emotions =
            listOf(
                WeeklyReportEmotion(keyword = "가족", percentage = 25),
                WeeklyReportEmotion(keyword = "감사", percentage = 75),
            ),
        // 키워드가 나온 완료 상태 — 1건 분석해 1건 성공 (#725).
        emotionAnalysis = EmotionAnalysis(total = 1, succeeded = 1, pending = 0, failed = 0),
    )
