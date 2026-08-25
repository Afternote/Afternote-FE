package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.core.domain.repository.UserRepository
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
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDay
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.presentation.model.DayBackground
import com.afternote.feature.mindrecord.presentation.model.DayContent
import com.afternote.feature.mindrecord.presentation.screen.receiver.ReceiverMindRecordScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DiaryScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.DraftListScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.WeeklyReportScreen
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DraftListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.MindRecordDraftLoader
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
import java.lang.reflect.Proxy
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class MindRecordLifecycleAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun diaryList_excludesDraftChangesMonthAndReloadsAfterDelete() {
        val currentMonth = YearMonth.now()
        val previousMonth = currentMonth.minusMonths(1)
        val currentPublished = diary(101L, "이번 달 공개 일기", currentMonth.atDay(3), isDraft = false)
        val currentDraft = diary(102L, "숨겨야 할 임시 일기", currentMonth.atDay(4), isDraft = true)
        val previousPublished = diary(103L, "지난달 삭제할 일기", previousMonth.atDay(5), isDraft = false)
        val repository =
            PrivateDiaryRepository().apply {
                lists[PrivateDiaryQuery(currentMonth.toString(), null)] =
                    DiaryList(listOf(currentPublished, currentDraft), 2, TodayMood.HAPPY)
                lists[PrivateDiaryQuery(previousMonth.toString(), null)] =
                    DiaryList(listOf(previousPublished), 1, TodayMood.SOSO)
            }
        val viewModel = DiaryListViewModel(repository)

        composeRule.setContent {
            AfternoteTheme {
                DiaryScreen(viewModel = viewModel)
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
        assertEquals(listOf(103L), repository.deleteCalls)
        assertEquals(
            listOf(
                PrivateDiaryQuery(currentMonth.toString(), null),
                PrivateDiaryQuery(previousMonth.toString(), null),
                PrivateDiaryQuery(previousMonth.toString(), null),
            ),
            repository.listCalls,
        )
        val state = viewModel.uiState.value as DiaryListUiState.Success
        assertEquals(previousMonth, state.yearMonth)
        assertTrue(state.diaries.isEmpty())
    }

    @Test
    fun combinedDraftList_routesSelectedAndAllDeletesToExactRepositories() {
        val currentMonth = YearMonth.now()
        val diaryRepository =
            PrivateDiaryRepository().apply {
                lists[PrivateDiaryQuery(currentMonth.toString(), true)] =
                    DiaryList(
                        diaries = listOf(diary(201L, "일기 임시저장", currentMonth.atDay(8), isDraft = true)),
                        monthDiaryCount = 1,
                        weeklyDominantMood = null,
                    )
            }
        val dailyQuestionRepository =
            PrivateDailyQuestionRepository(
                questions =
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
                loader = MindRecordDraftLoader(diaryRepository, dailyQuestionRepository),
                diaryRepository = diaryRepository,
                dailyQuestionRepository = dailyQuestionRepository,
                errorReporter = FakeErrorReporter(),
            )

        composeRule.setContent {
            AfternoteTheme {
                DraftListScreen(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("일기 임시저장").assertIsDisplayed()
        composeRule.onNodeWithText("질문 임시저장").assertIsDisplayed()
        composeRule.onNodeWithText("총 2개").assertIsDisplayed()

        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNode(hasText("질문 임시저장") and hasClickAction()).performClick()
        composeRule.onNodeWithText("총 1개 선택").assertIsDisplayed()
        composeRule.onNodeWithText("선택 삭제").performClick()
        composeRule
            .onNodeWithText("임시 저장된 기록이 삭제 되었습니다")
            .assertIsDisplayed()
        composeRule.onNodeWithText("질문 임시저장").assertDoesNotExist()
        composeRule.onNodeWithText("일기 임시저장").assertIsDisplayed()
        assertEquals(listOf(202L), dailyQuestionRepository.deleteCalls)
        assertTrue(diaryRepository.deleteCalls.isEmpty())

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            (viewModel.uiState.value as? DraftListUiState.Success)?.deleteOutcome == null
        }
        composeRule.onNodeWithText("선택").performClick()
        composeRule.onNodeWithText("전체 삭제").performClick()
        composeRule.onNodeWithText("임시 저장된 항목이 없습니다.").assertIsDisplayed()
        assertEquals(listOf(201L), diaryRepository.deleteCalls)
        assertEquals(listOf(202L), dailyQuestionRepository.deleteCalls)
        assertEquals(3, diaryRepository.listCalls.size)
        assertEquals(3, dailyQuestionRepository.listCalls.size)
        assertTrue((viewModel.uiState.value as DraftListUiState.Success).items.isEmpty())
    }

    @Test
    fun weeklyReport_errorRetry_mapsSparsePartialWeekWithoutIndexDrift() {
        val repository = PrivateWeeklyReportRepository()
        repository.results.addLast(Result.failure(IllegalStateException("weekly offline")))
        val userRepository = privateProfileRepository("테스트 사용자")
        val viewModel = WeeklyReportViewModel(repository, userRepository)

        composeRule.setContent {
            AfternoteTheme {
                WeeklyReportScreen(viewModel = viewModel)
            }
        }

        composeRule.onNodeWithText("weekly offline").assertIsDisplayed()
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
                            WeeklyReportDailyQuestion("같은 날 질문", "답변 A", wednesday.toString()),
                            WeeklyReportDailyQuestion("다른 날 질문", "답변 B", friday.toString()),
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
            PrivateMindRecordReceiverRepository(
                records =
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
            )
        val viewModel = ReceiverMindRecordViewModel(repository, FakeErrorReporter())

        composeRule.setContent {
            AfternoteTheme {
                ReceiverMindRecordScreen(viewModel = viewModel)
            }
        }
        composeRule.onNodeWithText("질문 범위 최신").assertIsDisplayed()
        composeRule.onNodeWithText("숨길 질문 draft").assertDoesNotExist()

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
        composeRule.onNodeWithText("마음의 기록").assertIsDisplayed()
        composeRule.onNodeWithText("범위 밖 일기").assertIsDisplayed()
        composeRule.onNodeWithText("데일리 질문").performClick()
        composeRule.onNodeWithText("범위 밖 질문").assertIsDisplayed()
        assertEquals(1, repository.getAllCalls)
        val reset = viewModel.uiState.value as ReceiverMindRecordUiState.Success
        assertFalse(reset.filter.isApplied)
        assertEquals(listOf(403L, 402L, 401L), reset.dailyQuestions.map(MindRecordSummary::id))
        assertEquals(listOf(413L, 412L, 411L), reset.diaries.map(MindRecordSummary::id))
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}

private data class PrivateDiaryQuery(
    val yearMonth: String,
    val draftOnly: Boolean?,
)

private class PrivateDiaryRepository : DiaryRepository {
    val lists = mutableMapOf<PrivateDiaryQuery, DiaryList>()
    val listCalls = mutableListOf<PrivateDiaryQuery>()
    val deleteCalls = mutableListOf<Long>()

    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> {
        val query = PrivateDiaryQuery(yearMonth, draftOnly)
        listCalls += query
        return Result.success(lists[query] ?: DiaryList(emptyList(), 0, null))
    }

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> = error("Unexpected diary create")

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> = error("Unexpected diary update")

    override suspend fun delete(id: Long): Result<Unit> {
        deleteCalls += id
        lists.replaceAll { _, list ->
            val remaining = list.diaries.filterNot { it.diaryId == id }
            list.copy(diaries = remaining, monthDiaryCount = remaining.size)
        }
        return Result.success(Unit)
    }
}

private data class PrivateDailyQuestionQuery(
    val date: String?,
    val draftOnly: Boolean?,
)

private class PrivateDailyQuestionRepository(
    questions: List<DailyQuestion>,
) : DailyQuestionRepository {
    private var questions = questions
    val listCalls = mutableListOf<PrivateDailyQuestionQuery>()
    val deleteCalls = mutableListOf<Long>()

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> {
        listCalls += PrivateDailyQuestionQuery(date, draftOnly)
        return Result.success(questions)
    }

    override suspend fun getToday(): Result<TodayDailyQuestion> = error("Unexpected today question load")

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> = error("Unexpected question create")

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = error("Unexpected question update")

    override suspend fun delete(id: Long): Result<Unit> {
        deleteCalls += id
        questions = questions.filterNot { it.dailyQuestionId == id }
        return Result.success(Unit)
    }
}

private class PrivateWeeklyReportRepository : WeeklyReportRepository {
    val results = ArrayDeque<Result<WeeklyReport>>()
    val requestedDates = mutableListOf<String>()

    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        requestedDates += date
        return requireNotNull(results.removeFirstOrNull())
    }
}

private class PrivateMindRecordReceiverRepository(
    private val records: ReceiverMindRecords,
) : MindRecordReceiverRepository {
    var getAllCalls = 0
        private set

    override suspend fun getAll(): Result<ReceiverMindRecords> {
        getAllCalls += 1
        return Result.success(records)
    }
}

@Suppress("UNCHECKED_CAST")
private fun privateProfileRepository(name: String): UserRepository =
    Proxy.newProxyInstance(
        UserRepository::class.java.classLoader,
        arrayOf(UserRepository::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getMyProfile" -> User(name = name, email = "test@afternote.local", phone = null, profileImageUrl = null)
            "toString" -> "PrivateProfileRepository"
            else -> error("Unexpected UserRepository call: ${method.name}")
        }
    } as UserRepository

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
