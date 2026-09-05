package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * 주간 범위 방어가 **한 곳**에서만 걸린다는 가드 (#547).
 *
 * 종전에는 집계(`countRecordedDays`)만 범위를 걸고 HISTORY 섹션은 `dailyQuestions` 를
 * 전량 렌더했다. 그래서 범위가 적용된 수치와 적용되지 않은 목록이 한 화면에 공존했다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyReportWeekRangeTest {
    private val dispatcher = StandardTestDispatcher()
    private val changeTracker = MindRecordChangeTracker()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `조회한 주 밖의 데일리질문은 수치와 목록 양쪽에서 빠진다`() =
        runTest(dispatcher) {
            // 이번 주(진입 시 기본 선택) 안 1건 + 지난 주 1건 + 다음 주 1건.
            val thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
            val report =
                report(
                    listOf(
                        dailyQuestion(thisMonday.plusDays(2)),
                        dailyQuestion(thisMonday.minusDays(3)),
                        dailyQuestion(thisMonday.plusDays(9)),
                    ),
                )

            val state = load(report)

            assertEquals("범위 안 1건만 남아야 한다", 1, state.dailyQuestions.size)
            assertEquals(thisMonday.plusDays(2), state.dailyQuestions.single().date)
            assertEquals("같은 목록을 세므로 수치도 1일이다", 1, state.recordedDays)
        }

    @Test
    fun `주 경계인 월요일과 일요일은 포함된다`() =
        runTest(dispatcher) {
            val thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
            val report =
                report(listOf(dailyQuestion(thisMonday), dailyQuestion(thisMonday.plusDays(6))))

            val state = load(report)

            assertEquals(2, state.dailyQuestions.size)
            assertEquals(2, state.recordedDays)
        }

    @Test
    fun `표시 목록과 집계가 같은 목록을 본다`() =
        runTest(dispatcher) {
            // 범위 밖 항목이 섞여 있어도 두 값이 어긋나지 않는다.
            val thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
            val report =
                report(
                    listOf(
                        dailyQuestion(thisMonday.plusDays(1)),
                        dailyQuestion(thisMonday.plusDays(1)),
                        dailyQuestion(thisMonday.minusWeeks(2)),
                    ),
                )

            val state = load(report)

            assertEquals("같은 날 2건은 목록에 2건", 2, state.dailyQuestions.size)
            assertEquals("기록일수는 날짜 단위라 1일", 1, state.recordedDays)
            // 최상단 카드도 같은 목록을 센다. 서버 원본(dailyQuestionAmount = 3)을 그대로
            // 쓰면 목록 2건 · 카드 3 으로 갈려, 같은 불일치가 방향만 뒤집힌 채 남는다.
            assertEquals(
                "카드 수치도 필터를 탄다",
                2,
                state.counts.first { it.second == MindRecordCategoryUi.DailyQuestion }.first,
            )
        }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    private fun dailyQuestion(date: LocalDate) = WeeklyReportDailyQuestion(title = "질문", content = "답변", date = date)

    private fun report(dailyQuestions: List<WeeklyReportDailyQuestion>) =
        WeeklyReport(
            dailyQuestionAmount = dailyQuestions.size,
            diaryAmount = 0,
            summaryText = "",
            week = emptyList(),
            dailyQuestions = dailyQuestions,
            emotions = emptyList(),
            emotionAnalysis = EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0),
        )

    private suspend fun kotlinx.coroutines.test.TestScope.load(report: WeeklyReport): WeeklyReportUiState.Success {
        val repository =
            object : WeeklyReportRepository {
                override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> = Result.success(report)
            }
        val viewModel =
            WeeklyReportViewModel(ObserveWeeklyReportUseCase(repository, userRepository()), changeTracker, RecordingErrorReporter())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel.uiState.value as WeeklyReportUiState.Success
    }

    private fun userRepository(): FakeUserRepository =
        FakeUserRepository.strict().apply {
            onReceiverListFlow = { flowOf(emptyList()) }
            onGetMyProfile = { User(name = "adamtia", email = "a@b.c", phone = null, profileImageUrl = null) }
        }
}
