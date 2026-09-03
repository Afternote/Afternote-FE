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
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeUserRepository
import com.afternote.afternote_fe.test.testReceiver
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.model.WeeklyReportEmotion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.mindrecord.presentation.screen.memoryspace.MemorySpaceScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionAnswerListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DailyQuestionWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryWriteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DraftListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.WeeklyReportScreen
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel
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
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
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
        val repository = CompletionTimeLetterRepository()
        val userRepository = FakeUserRepository(receivers = listOf(testReceiver()))
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
            viewModel.uiState.value.error == TimeLetterWriteError.RECIPIENT_REQUIRED
        }
        composeRule.onNodeWithText("수신자를 선택해주세요.").assertIsDisplayed()

        composeRule.waitUntil(timeoutMillis = TIMEOUT) { userRepository.getReceiversCalls == 1 }
        composeRule.runOnIdle {
            viewModel.clearError()
            viewModel.setRecipients(listOf(7L))
        }
        register.performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.error == TimeLetterWriteError.SEND_DATE_REQUIRED
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

        composeRule.runOnIdle { repository.allowCreate.complete(Unit) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.registered && !viewModel.uiState.value.isSaving
        }
        assertEquals(1, repository.registeredListCalls)
        assertEquals(1, repository.createCalls.size)
    }

    @Test
    fun timeLetterRecipientSelector_roundTripPreservesTitleTextAndExactReceiverId() {
        val repository = CompletionTimeLetterRepository()
        val userRepository = FakeUserRepository(receivers = listOf(testReceiver()))
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
        val repository =
            CompletionTimeLetterRepository(
                nextDetailGate = firstLoad,
                registeredLetters = TimeLetterList(listOf(detailLetter), 1),
            )
        repository.detailResults.addLast(Result.success(completionDetailLetter()))
        val userRepository = FakeUserRepository(receivers = listOf(testReceiver()))
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
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { repository.detailIds == listOf(509L) }
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
        assertEquals(listOf(509L, 509L), repository.detailIds)
        assertEquals(1, repository.registeredListCalls)
        assertEquals(3, userRepository.getReceiversCalls)
    }

    @Test
    fun mindRecordHome_dailyQuestionLoadingEmptyAndErrorRetrySuccess_areRendered() {
        val emptyGate = CompletableDeferred<Result<List<DailyQuestion>>>()
        val emptyRepository = CompletionDailyQuestionRepository(nextListGate = emptyGate)
        var activeViewModel by mutableStateOf(DailyQuestionListViewModel(emptyRepository))

        composeRule.setContent {
            AfternoteTheme {
                DailyQuestionAnswerListScreen(viewModel = activeViewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) { emptyRepository.listCalls == 1 }
        assertEquals(DailyQuestionListUiState.Loading, activeViewModel.uiState.value)
        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()

        composeRule.runOnIdle { emptyGate.complete(Result.success(emptyList())) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (activeViewModel.uiState.value as? DailyQuestionListUiState.Success)?.answers?.isEmpty() == true
        }
        composeRule.onNodeWithText("아직 등록된 답변이 없어요.").assertIsDisplayed()

        val retryRepository =
            CompletionDailyQuestionRepository().apply {
                listResults.addLast(Result.failure(IllegalStateException("home offline")))
            }
        val retryViewModel = DailyQuestionListViewModel(retryRepository)
        composeRule.runOnIdle { activeViewModel = retryViewModel }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            retryViewModel.uiState.value is DailyQuestionListUiState.Error
        }
        composeRule.onNodeWithText("home offline").assertIsDisplayed()

        retryRepository.listResults.addLast(
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
        assertEquals(2, retryRepository.listCalls)
        assertEquals(2, retryRepository.todayCalls)
    }

    @Test
    fun dailyQuestionWrite_successRefreshesExistingListWithCreatedAnswer() {
        val repository = CompletionDailyQuestionRepository()
        val listViewModel = DailyQuestionListViewModel(repository)
        var writeViewModel by mutableStateOf<DailyQuestionWriteViewModel?>(null)
        var submitSuccessCalls = 0

        composeRule.setContent {
            val activeWriteViewModel = writeViewModel
            AfternoteTheme {
                if (activeWriteViewModel == null) {
                    DailyQuestionAnswerListScreen(viewModel = listViewModel)
                } else {
                    DailyQuestionWriteScreen(
                        viewModel = activeWriteViewModel,
                        onSubmitSuccess = {
                            submitSuccessCalls += 1
                            writeViewModel = null
                            listViewModel.refreshOnReturn()
                        },
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (listViewModel.uiState.value as? DailyQuestionListUiState.Success)?.answers?.isEmpty() == true
        }
        composeRule.onNodeWithText("아직 등록된 답변이 없어요.").assertIsDisplayed()

        composeRule.runOnIdle {
            writeViewModel =
                DailyQuestionWriteViewModel(
                    repository = repository,
                    photoUploadRepository = CompletionPhotoUploadRepository,
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
        composeRule.onNodeWithText("오늘의 테스트 질문").assertIsDisplayed()
        composeRule.onNodeWithText("작성 후 목록에 반영될 답변").assertIsDisplayed()
        assertEquals(1, repository.createCalls.size)
        assertEquals(71L, repository.createCalls.single().questionId)
        assertEquals(false, repository.createCalls.single().isDraft)
        assertEquals(2, repository.listCalls)
        assertEquals(3, repository.todayCalls)
    }

    @Test
    fun diaryDraftRow_routesIdAndMonthThenPrefillsAndPublishesWithPatch() {
        val currentMonth = YearMonth.now()
        val draftDate = currentMonth.atDay(12)
        val repository =
            CompletionDiaryRepository(
                drafts =
                    mutableListOf(
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
        val draftListViewModel =
            DraftListViewModel(
                diaryRepository = repository,
                dailyQuestionRepository = CompletionDailyQuestionRepository(),
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
                                    route =
                                        MindRecordRoute.DiaryWriteRoute(
                                            draftId = draftId,
                                            draftYearMonth = draftYearMonth,
                                        ),
                                    repository = repository,
                                    photoUploadRepository = CompletionPhotoUploadRepository,
                                    userRepository = FakeUserRepository(),
                                )
                        },
                    )
                } else {
                    DiaryWriteScreen(
                        viewModel = activeWriteViewModel,
                        onSubmitSuccess = { submitSuccessCalls += 1 },
                    )
                }
            }
        }

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
        assertEquals("https://afternote.test/draft.jpg", loaded.imageUrl)
        composeRule.onNodeWithText("이어 쓸 일기").assertIsDisplayed()

        composeRule.runOnIdle {
            requireNotNull(writeViewModel).onTitleChanged("완성한 일기")
            requireNotNull(writeViewModel).onContentChanged("<p>완성한 본문</p>")
        }
        composeRule.onNode(hasText("등록") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { submitSuccessCalls == 1 }

        val update = repository.updateCalls.single()
        assertEquals(612L, update.first)
        assertEquals("완성한 일기", update.second.title)
        assertEquals("<p>완성한 본문</p>", update.second.content)
        assertEquals(false, update.second.isDraft)
        assertEquals(TodayMood.SOSO, update.second.todayMood)
        assertEquals(draftDate.toString(), update.second.date)
        assertEquals("https://afternote.test/draft.jpg", update.second.imageUrl)
        assertTrue(repository.createCalls.isEmpty())
        assertEquals(2, repository.listCalls.size)
        assertTrue(repository.listCalls.all { it == currentMonth.toString() to true })
    }

    @Test
    fun memorySpace_supportedSuccess_opensAndClosesDetailThenNavigatesBack() {
        val viewModel = MemorySpaceViewModel()
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
        composeRule.onNodeWithContentDescription("기억 1").performClick()
        composeRule
            .onNodeWithText("이 순간은 나에게 특별한 의미가 있었습니다.", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("#평온").assertIsDisplayed()
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
            CompletionWeeklyReportRepository().apply {
                results.addLast(Result.failure(IllegalStateException("weekly offline")))
            }
        val userRepository =
            FakeUserRepository(
                profile = User("주간 사용자", "weekly@afternote.local", null, null),
                receivers = emptyList(),
            )
        val viewModel = WeeklyReportViewModel(repository, userRepository)

        composeRule.setContent {
            AfternoteTheme {
                WeeklyReportScreen(viewModel = viewModel)
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value is WeeklyReportUiState.Error
        }
        composeRule.onNodeWithText("weekly offline").assertIsDisplayed()
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
        repository: CompletionTimeLetterRepository,
        userRepository: FakeUserRepository,
    ): TimeLetterWriteViewModel {
        val resolver = ResolveTimeLetterBlocksUseCase(CompletionPhotoUploadRepository)
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolver),
            resolveTimeLetterBlocksUseCase = resolver,
            timeLetterRepository = repository,
            userRepository = userRepository,
            fileMetadataRepository = CompletionFileMetadataRepository,
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to null)),
        )
    }

    private companion object {
        const val TIMEOUT = 5_000L

        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}

private data class CompletionTimeLetterCreateCall(
    val title: String?,
    val blocks: List<NewTimeLetterBlock>,
    val sendAt: String?,
    val deliveryMode: TimeLetterDeliveryMode,
    val status: TimeLetterStatus,
    val receiverIds: List<Long>,
)

private class CompletionTimeLetterRepository(
    var nextDetailGate: CompletableDeferred<Result<TimeLetter>>? = null,
    var registeredLetters: TimeLetterList = TimeLetterList(emptyList(), 0),
) : TimeLetterRepository {
    val allowCreate = CompletableDeferred<Unit>()
    val createCalls = mutableListOf<CompletionTimeLetterCreateCall>()
    val detailResults = ArrayDeque<Result<TimeLetter>>()
    val detailIds = mutableListOf<Long>()
    var registeredListCalls = 0
        private set

    override suspend fun getTimeLetters(): TimeLetterList {
        registeredListCalls += 1
        return registeredLetters
    }

    override suspend fun getTemporaryTimeLetters(): TimeLetterList = TimeLetterList(emptyList(), 0)

    override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter {
        detailIds += timeLetterId
        val gate = nextDetailGate
        nextDetailGate = null
        return (gate?.await() ?: requireNotNull(detailResults.removeFirstOrNull())).getOrThrow()
    }

    override suspend fun createTimeLetter(
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode,
        status: TimeLetterStatus,
        receiverIds: List<Long>,
    ): TimeLetter {
        createCalls += CompletionTimeLetterCreateCall(title, blocks, sendAt, deliveryMode, status, receiverIds)
        allowCreate.await()
        return TimeLetter(
            id = 901L,
            title = title,
            sendAt = sendAt,
            deliveredAt = null,
            status = status,
            blocks = emptyList(),
            receiverIds = receiverIds,
        )
    }

    override suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode?,
        status: TimeLetterStatus?,
    ): TimeLetter = error("Unexpected time-letter update: $timeLetterId")

    override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
        error("Unexpected time-letter delete: $timeLetterIds")
    }

    override suspend fun deleteAllTemporary() {
        error("Unexpected delete-all-drafts")
    }
}

private object CompletionPhotoUploadRepository : PhotoUploadRepository {
    override suspend fun upload(
        uriString: String,
        directory: String,
    ): Result<String> = error("Unexpected upload: $uriString")
}

private object CompletionFileMetadataRepository : FileMetadataRepository {
    override suspend fun getFileName(uriString: String): String = error("Unexpected file-name lookup: $uriString")

    override suspend fun getMimeType(uriString: String): String? = error("Unexpected MIME lookup: $uriString")
}

private class CompletionDailyQuestionRepository(
    var nextListGate: CompletableDeferred<Result<List<DailyQuestion>>>? = null,
) : DailyQuestionRepository {
    val listResults = ArrayDeque<Result<List<DailyQuestion>>>()
    val storedAnswers = mutableListOf<DailyQuestion>()
    val createCalls = mutableListOf<DailyQuestionCreatePayload>()
    val updateCalls = mutableListOf<Pair<Long, DailyQuestionUpdatePayload>>()
    var today = TodayDailyQuestion(71L, 71, "오늘의 테스트 질문", false)
    var listCalls = 0
        private set
    var todayCalls = 0
        private set

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> {
        listCalls += 1
        val gate = nextListGate
        nextListGate = null
        return gate?.await() ?: listResults.removeFirstOrNull() ?: Result.success(storedAnswers.toList())
    }

    override suspend fun getToday(): Result<TodayDailyQuestion> {
        todayCalls += 1
        return Result.success(today)
    }

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit> {
        createCalls += payload
        storedAnswers +=
            DailyQuestion(
                dailyQuestionId = 800L + createCalls.size,
                title = today.content,
                content = payload.content,
                createdAt = "2026-08-22",
                imageUrl = payload.imageUrl,
                isDraft = payload.isDraft,
            )
        today = today.copy(isAnswered = !payload.isDraft, isDraft = payload.isDraft)
        return Result.success(Unit)
    }

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Unit> {
        updateCalls += id to payload
        storedAnswers.replaceAll { answer ->
            if (answer.dailyQuestionId == id) {
                answer.copy(
                    content = payload.content ?: answer.content,
                    imageUrl = payload.imageUrl ?: answer.imageUrl,
                    isDraft = payload.isDraft ?: answer.isDraft,
                )
            } else {
                answer
            }
        }
        return Result.success(Unit)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        storedAnswers.removeAll { it.dailyQuestionId == id }
        return Result.success(Unit)
    }
}

private class CompletionDiaryRepository(
    val drafts: MutableList<Diary>,
) : DiaryRepository {
    val listCalls = mutableListOf<Pair<String, Boolean?>>()
    val createCalls = mutableListOf<DiaryCreatePayload>()
    val updateCalls = mutableListOf<Pair<Long, DiaryUpdatePayload>>()

    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> {
        listCalls += yearMonth to draftOnly
        val matching = drafts.filter { draftOnly == null || it.isDraft == draftOnly }
        return Result.success(
            DiaryList(
                diaries = matching,
                monthDiaryCount = matching.size,
                weeklyDominantMood = matching.firstOrNull()?.todayMood,
            ),
        )
    }

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> {
        createCalls += payload
        return Result.success(Unit)
    }

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> {
        updateCalls += id to payload
        drafts.replaceAll { diary ->
            if (diary.diaryId == id) {
                diary.copy(
                    title = payload.title,
                    content = payload.content,
                    date = payload.date,
                    todayMood = payload.todayMood,
                    imageUrl = payload.imageUrl,
                    isDraft = payload.isDraft,
                )
            } else {
                diary
            }
        }
        return Result.success(Unit)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        drafts.removeAll { it.diaryId == id }
        return Result.success(Unit)
    }
}

private class CompletionWeeklyReportRepository : WeeklyReportRepository {
    val results = ArrayDeque<Result<WeeklyReport>>()
    val requestedDates = mutableListOf<String>()

    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        requestedDates += date
        return requireNotNull(results.removeFirstOrNull()) { "Missing weekly response for $date" }
    }
}

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
                    emotion = TodayMood.HAPPY,
                ),
            ),
        dailyQuestions =
            listOf(
                WeeklyReportDailyQuestion(
                    title = "화요일의 질문",
                    content = "화요일의 답변",
                    date = monday.plusDays(1).toString(),
                ),
            ),
        emotions =
            listOf(
                WeeklyReportEmotion(keyword = "가족", percentage = 25),
                WeeklyReportEmotion(keyword = "감사", percentage = 75),
            ),
    )
