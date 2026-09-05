package com.afternote.feature.mindrecord.presentation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
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
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.domain.testing.FakeMindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.unexpectedCall
import com.afternote.feature.mindrecord.presentation.model.DayBackground
import com.afternote.feature.mindrecord.presentation.model.DayContent
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.screen.receiver.ReceiverMindRecordScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DraftListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.WeeklyReportScreen
import com.afternote.feature.mindrecord.presentation.usecase.DeleteMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordFilter
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SortOrder
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class MindRecordLifecycleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun diaryList_excludesDraftChangesMonthAndReloadsAfterDelete() {
        val currentMonth = YearMonth.now()
        val previousMonth = currentMonth.minusMonths(1)
        val currentPublished = diary(101L, "이번 달 공개 일기", currentMonth.atDay(3), isDraft = false)
        val currentDraft = diary(102L, "숨겨야 할 임시 일기", currentMonth.atDay(4), isDraft = true)
        val previousPublished = diary(103L, "지난달 삭제할 일기", previousMonth.atDay(5), isDraft = false)
        val diaryLists =
            mutableMapOf(
                FakeDiaryRepository.ListQuery(currentMonth.toString(), null) to
                    DiaryList(listOf(currentPublished, currentDraft), 2, TodayMood.HAPPY),
                FakeDiaryRepository.ListQuery(previousMonth.toString(), null) to
                    DiaryList(listOf(previousPublished), 1, TodayMood.SOSO),
            )
        val repository = scriptedDiaryRepository(diaryLists)
        val viewModel = DiaryListViewModel(repository, MindRecordChangeTracker(), RecordingErrorReporter())

        composeRule.setContent {
            AfternoteTheme {
                DiaryScreen(
                    viewModel = viewModel,
                    onItemClick = { _, _ -> },
                    onEditClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("이번 달 공개 일기").assertIsDisplayed()
        composeRule.onNodeWithText("숨겨야 할 임시 일기").assertDoesNotExist()
        composeRule
            .onNodeWithText("${currentMonth.year}년 ${currentMonth.monthValue}월")
            .assertIsDisplayed()

        composeRule.runOnIdle { viewModel.selectYearMonth(previousMonth) }
        composeRule.onNodeWithText("지난달 삭제할 일기").assertIsDisplayed()
        composeRule.onNodeWithText("이번 달 공개 일기").assertDoesNotExist()
        composeRule
            .onNodeWithText("${previousMonth.year}년 ${previousMonth.monthValue}월")
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription("더보기 메뉴").performClick()
        composeRule.onNodeWithText("삭제하기").performClick()
        composeRule.onNodeWithText("아직 등록된 일기가 없어요.").assertIsDisplayed()
        assertEquals(listOf(103L), repository.deletedIds)
        assertEquals(
            listOf(
                FakeDiaryRepository.ListQuery(currentMonth.toString(), null),
                FakeDiaryRepository.ListQuery(previousMonth.toString(), null),
                FakeDiaryRepository.ListQuery(previousMonth.toString(), null),
            ),
            repository.listQueries,
        )
        val state = viewModel.uiState.value as DiaryListUiState.Success
        assertEquals(previousMonth, state.yearMonth)
        assertTrue(state.diaries.isEmpty())
    }

    @Test
    fun combinedDraftList_routesSelectedAndAllDeletesToExactRepositories() {
        val currentMonth = YearMonth.now()
        val diaryLists =
            mutableMapOf(
                FakeDiaryRepository.ListQuery(currentMonth.toString(), true) to
                    DiaryList(
                        diaries = listOf(diary(201L, "일기 임시저장", currentMonth.atDay(8), isDraft = true)),
                        monthDiaryCount = 1,
                        weeklyDominantMood = null,
                    ),
            )
        val diaryRepository = scriptedDiaryRepository(diaryLists)
        val dailyQuestionRepository =
            FakeDailyQuestionRepository(
                // 이 시나리오는 임시저장 목록의 조회·삭제만 태운다.
                onGetToday = { unexpectedCall("DailyQuestionRepository.getToday") },
                onCreate = { unexpectedCall("DailyQuestionRepository.create") },
                onUpdate = { _, _ -> unexpectedCall("DailyQuestionRepository.update") },
                initialAnswers =
                    listOf(
                        DailyQuestion(
                            dailyQuestionId = 202L,
                            title = "질문 임시저장",
                            content = "임시 답변",
                            createdAt = currentMonth.atDay(9).toString(),
                            isDraft = true,
                        ),
                    ),
            )
        val viewModel =
            DraftListViewModel(
                loadDrafts = LoadMindRecordDraftsUseCase(diaryRepository, dailyQuestionRepository),
                deleteDrafts = DeleteMindRecordDraftsUseCase(diaryRepository, dailyQuestionRepository),
                errorReporter = RecordingErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                DraftListScreen(
                    viewModel = viewModel,
                    onBackClick = {},
                    onDailyQuestionDraftClick = {},
                    onDiaryDraftClick = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("일기 임시저장").assertIsDisplayed()
        composeRule.onNodeWithText("질문 임시저장").assertIsDisplayed()
        composeRule.onNodeWithText("총 2개").assertIsDisplayed()
        val initialDiaryQueryCount = diaryRepository.listQueries.size
        val initialDailyQuestionQueryCount = dailyQuestionRepository.listQueries.size

        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNode(hasText("질문 임시저장") and hasClickAction()).performClick()
        composeRule.onNodeWithText("총 1개 선택").assertIsDisplayed()
        composeRule.onNodeWithText("선택 삭제").performClick()
        composeRule
            .onNodeWithText("임시 저장된 기록이 삭제 되었습니다")
            .assertIsDisplayed()
        composeRule.onNodeWithText("질문 임시저장").assertDoesNotExist()
        composeRule.onNodeWithText("일기 임시저장").assertIsDisplayed()
        assertEquals(listOf(202L), dailyQuestionRepository.deletedIds)
        assertTrue(diaryRepository.deletedIds.isEmpty())

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (viewModel.uiState.value as? DraftListUiState.Success)?.deleteOutcome == null
        }
        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNodeWithText("전체 삭제").performClick()
        composeRule.onNodeWithText("임시 저장된 항목이 없습니다.").assertIsDisplayed()
        assertEquals(listOf(201L), diaryRepository.deletedIds)
        assertEquals(listOf(202L), dailyQuestionRepository.deletedIds)
        assertEquals(initialDiaryQueryCount + 2, diaryRepository.listQueries.size)
        assertTrue(
            diaryRepository.listQueries.all {
                it == FakeDiaryRepository.ListQuery(currentMonth.toString(), true)
            },
        )
        assertEquals(initialDailyQuestionQueryCount + 2, dailyQuestionRepository.listQueries.size)
        assertTrue(
            dailyQuestionRepository.listQueries.all {
                it == FakeDailyQuestionRepository.ListQuery(date = null, draftOnly = true)
            },
        )
        assertTrue((viewModel.uiState.value as DraftListUiState.Success).items.isEmpty())
    }

    @Test
    fun weeklyReport_errorRetry_mapsSparsePartialWeekWithoutIndexDrift() {
        val repository = FakeWeeklyReportRepository()
        repository.results.addLast(Result.failure(IllegalStateException("weekly offline")))
        val userRepository = privateProfileRepository("테스트 사용자")
        val viewModel =
            WeeklyReportViewModel(
                ObserveWeeklyReportUseCase(repository, userRepository),
                MindRecordChangeTracker(),
                RecordingErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                WeeklyReportScreen(viewModel = viewModel)
            }
        }

        // 이 자리는 종전에 `"weekly offline"`— 즉 **예외 원문**— 이 화면에 뜨는 것을 단언했다.
        // 그게 이 테스트의 관심사(재조회 뒤 주차 인덱스가 밀리지 않는가)가 아니었을 뿐 아니라,
        // 서버 오류 원문을 화면에 내지 않는다는 저장소 규약(#1339)과 정반대라 결함을 고정하고
        // 있었다. 오류 화면에 들어섰다는 사실은 안내 문자열로 확인한다 (#1882).
        val failureCopy =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getString(R.string.mindrecord_error_weekly_report_failed)
        composeRule.onNodeWithText(failureCopy).assertIsDisplayed()
        composeRule.onNodeWithText("weekly offline").assertDoesNotExist()
        assertEquals(1, repository.requestedDates.size)
        val requestedMonday = LocalDate.parse(repository.requestedDates.single())
        val wednesday = requestedMonday.plusDays(2)
        val friday = requestedMonday.plusDays(4)
        repository.results.addLast(
            Result.success(
                WeeklyReport(
                    dailyQuestionAmount = 2,
                    diaryAmount = 1,
                    summaryText = "",
                    week =
                        listOf(
                            WeeklyReportDay(
                                diaryId = 301L,
                                day = wednesday.dayOfMonth,
                                isDiary = true,
                                countsAsRecord = true,
                                emotion = TodayMood.HAPPY,
                            ),
                            WeeklyReportDay(
                                diaryId = 302L,
                                day = wednesday.dayOfMonth,
                                isDiary = false,
                                countsAsRecord = true,
                                emotion = null,
                            ),
                        ),
                    dailyQuestions =
                        listOf(
                            WeeklyReportDailyQuestion("같은 날 질문", "답변 A", wednesday),
                            WeeklyReportDailyQuestion("다른 날 질문", "답변 B", friday),
                        ),
                    emotions = emptyList(),
                    // 이 테스트들은 분석 상태를 보지 않는다 — 완료로 고정한다 (#725).
                    emotionAnalysis = EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0),
                ),
            ),
        )

        composeRule.runOnIdle { viewModel.selectWeek(requestedMonday) }
        composeRule.onNodeWithText("WEEKLY SUMMARY").assertIsDisplayed()
        val success = viewModel.uiState.value as WeeklyReportUiState.Success
        assertEquals(7, success.weekDays.size)
        assertEquals(DayOfWeek.entries.toList(), success.weekDays.map { it.dayOfWeek })
        assertEquals(DayContent.EmojiOnly("😊"), success.weekDays[2].content)
        assertEquals(DayBackground.Green, success.weekDays[2].background)
        assertEquals(2, success.recordedDays)
        assertEquals(emptyList<Any>(), success.emotionKeywords)
        assertEquals(2, success.dailyQuestions.size)
        assertEquals(listOf(requestedMonday.toString(), requestedMonday.toString()), repository.requestedDates)
    }

    @Test
    fun receiverQuestionAndDiary_filterThenReset_reusesSingleRepositorySnapshot() {
        val repository =
            FakeMindRecordReceiverRepository(
                result =
                    Result.success(
                        ReceiverMindRecords(
                            dailyQuestions =
                                listOf(
                                    record(401L, MindRecordType.DAILY_QUESTION, "범위 밖 질문", "2026-07-01"),
                                    record(402L, MindRecordType.DAILY_QUESTION, "질문 범위 시작", "2026-08-12"),
                                    record(403L, MindRecordType.DAILY_QUESTION, "질문 범위 최신", "2026-08-20"),
                                    record(404L, MindRecordType.DAILY_QUESTION, "숨길 질문 draft", "2026-08-21", true),
                                ),
                            diaries =
                                listOf(
                                    record(411L, MindRecordType.DIARY, "범위 밖 일기", "2026-07-02"),
                                    record(412L, MindRecordType.DIARY, "일기 범위 시작", "2026-08-13"),
                                    record(413L, MindRecordType.DIARY, "일기 범위 최신", "2026-08-19"),
                                    record(414L, MindRecordType.DIARY, "숨길 일기 draft", "2026-08-22", true),
                                ),
                        ),
                    ),
            )
        val viewModel = ReceiverMindRecordViewModel(repository, RecordingErrorReporter())

        composeRule.setContent {
            AfternoteTheme {
                ReceiverMindRecordScreen(
                    viewModel = viewModel,
                    onBackClick = {},
                )
            }
        }
        composeRule.onNodeWithText("질문 범위 최신").assertIsDisplayed()
        composeRule.onNodeWithText("숨길 질문 draft").assertDoesNotExist()
        val initialGetAllCalls = repository.getAllCalls

        composeRule.runOnIdle {
            viewModel.applyFilter(
                ReceiverMindRecordFilter(
                    sortOrder = SortOrder.OLDEST,
                    fromDate = "2026-08-10",
                    toDate = "2026-08-31",
                ),
            )
        }
        composeRule.onNodeWithText("2026-08-10 - 2026-08-31").assertIsDisplayed()
        composeRule.onNodeWithText("과거순").assertIsDisplayed()
        composeRule.onNodeWithText("범위 밖 질문").assertDoesNotExist()
        val filtered = viewModel.uiState.value as ReceiverMindRecordUiState.Success
        assertEquals(listOf(402L, 403L), filtered.dailyQuestions.map(MindRecordSummary::id))
        assertEquals(listOf(412L, 413L), filtered.diaries.map(MindRecordSummary::id))

        composeRule.onNodeWithText("일기").performClick()
        composeRule.onNodeWithText("일기 범위 시작").assertIsDisplayed()
        composeRule.onNodeWithText("범위 밖 일기").assertDoesNotExist()
        composeRule.onNodeWithText("숨길 일기 draft").assertDoesNotExist()

        composeRule
            .onNode(hasText("2026-08-10 - 2026-08-31") and hasClickAction())
            .performClick()
        composeRule.onNodeWithText("초기화").performClick()
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (viewModel.uiState.value as? ReceiverMindRecordUiState.Success)?.filter?.isApplied == false
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2026-08-10 - 2026-08-31").assertDoesNotExist()
        composeRule.onNodeWithText("범위 밖 일기").assertIsDisplayed()
        composeRule.onNodeWithText("데일리 질문").performClick()
        composeRule.onNodeWithText("범위 밖 질문").assertIsDisplayed()
        assertEquals(initialGetAllCalls, repository.getAllCalls)
        val reset = viewModel.uiState.value as ReceiverMindRecordUiState.Success
        assertFalse(reset.filter.isApplied)
        assertEquals(listOf(403L, 402L, 401L), reset.dailyQuestions.map(MindRecordSummary::id))
        assertEquals(listOf(413L, 412L, 411L), reset.diaries.map(MindRecordSummary::id))
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}

/** 달·임시저장 여부마다 다른 목록을 돌려주는 조회. 삭제는 그 목록들에서 함께 지운다. */
private fun scriptedDiaryRepository(lists: MutableMap<FakeDiaryRepository.ListQuery, DiaryList>) =
    FakeDiaryRepository(
        onGetList = { yearMonth, draftOnly ->
            Result.success(
                lists[FakeDiaryRepository.ListQuery(yearMonth, draftOnly)] ?: DiaryList(emptyList(), 0, null),
            )
        },
        onCreate = { unexpectedCall("DiaryRepository.create") },
        onUpdate = { _, _ -> unexpectedCall("DiaryRepository.update") },
        onDelete = { id ->
            lists.replaceAll { _, list ->
                val remaining = list.diaries.filterNot { it.diaryId == id }
                list.copy(diaries = remaining, monthDiaryCount = remaining.size)
            }
            Result.success(Unit)
        },
    )

private fun privateProfileRepository(name: String): FakeUserRepository =
    FakeUserRepository.strict().apply {
        onGetMyProfile = {
            User(name = name, email = "test@afternote.local", phone = null, profileImageUrl = null)
        }
    }

private fun diary(
    id: Long,
    title: String,
    date: LocalDate,
    isDraft: Boolean,
): Diary =
    Diary(
        diaryId = id,
        title = title,
        content = "본문 $id",
        date = date.toString(),
        createdAt = date.toString(),
        todayMood = TodayMood.HAPPY,
        isDraft = isDraft,
    )

private fun record(
    id: Long,
    type: MindRecordType,
    title: String,
    date: String,
    isDraft: Boolean = false,
): MindRecordSummary =
    MindRecordSummary(
        id = id,
        type = type,
        title = title,
        content = "본문 $id",
        recordDate = date,
        isDraft = isDraft,
        createdAt = date,
    )
