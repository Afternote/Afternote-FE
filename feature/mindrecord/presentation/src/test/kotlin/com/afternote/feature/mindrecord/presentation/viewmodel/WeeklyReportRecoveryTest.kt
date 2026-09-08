package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 주차 조회 실패 뒤 복구 경로 가드 (#723).
 *
 * 종전에는 실패하면 화면 전체가 오류 문구 하나로 바뀌어 **리포트도 주차 선택 UI 도
 * 사라졌다.** 재진입 자동 갱신도 실패한 주차를 잊고 이번 주를 다시 불러, 나갔다 들어와도
 * 사용자가 보려던 주차로 돌아오지 못했다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyReportRecoveryTest {
    private val dispatcher = StandardTestDispatcher()
    private val changeTracker = MindRecordChangeTracker()
    private val thisMonday: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)
    private val lastMonday: LocalDate = thisMonday.minusWeeks(1)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `주차 변경이 실패해도 직전 리포트와 주차 선택이 남는다`() =
        runTest(dispatcher) {
            val repository = ScriptedRepository(mapOf(thisMonday to Result.success(report())))
            val viewModel = start(repository)

            viewModel.selectWeek(lastMonday) // 이 주차는 스크립트에 없어 실패한다
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue("오류 화면으로 갈아치우지 않는다", state is WeeklyReportUiState.Success)
            state as WeeklyReportUiState.Success
            assertNotNull("실패를 배너로 알린다", state.loadFailure)
            assertEquals("못 불러온 주차를 밝힌다", lastMonday, state.loadFailure?.failedWeekLabel)
            assertTrue("주차 선택 수단이 남는다", state.weekOptions.isNotEmpty())
        }

    @Test
    fun `첫 조회부터 실패해도 주차 선택과 실패한 주차가 남는다`() =
        runTest(dispatcher) {
            val repository = ScriptedRepository(emptyMap())
            val viewModel = start(repository)

            val state = viewModel.uiState.value
            assertTrue(state is WeeklyReportUiState.Error)
            state as WeeklyReportUiState.Error
            assertTrue("이 화면에서 빠져나갈 수단이 있어야 한다", state.weekOptions.isNotEmpty())
            assertEquals(thisMonday, state.failedMonday)
        }

    @Test
    fun `재진입 자동 갱신은 이번 주가 아니라 실패한 주차를 다시 부른다`() =
        runTest(dispatcher) {
            val repository = ScriptedRepository(mapOf(thisMonday to Result.success(report())))
            val viewModel = start(repository)
            viewModel.selectWeek(lastMonday)
            advanceUntilIdle()

            // 화면에 들어온 첫 ON_RESUME 은 init 조회가 덮으므로 건너뛴다 (#736 리뷰).
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            // 나갔다 돌아옴 — 그 사이 서버가 복구됐다고 본다.
            repository.script = mapOf(lastMonday to Result.success(report(diaryAmount = 7)))
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success
            assertEquals("사용자가 보려던 주차로 복구된다", lastMonday, state.selectedMonday)
            assertNull("복구되면 배너는 사라진다", state.loadFailure)
        }

    @Test
    fun `재시도는 실패한 주차를 그대로 다시 부른다`() =
        runTest(dispatcher) {
            val repository = ScriptedRepository(mapOf(thisMonday to Result.success(report())))
            val viewModel = start(repository)
            viewModel.selectWeek(lastMonday)
            advanceUntilIdle()

            repository.script = mapOf(lastMonday to Result.success(report()))
            viewModel.retry()
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success
            assertEquals(lastMonday, state.selectedMonday)
            assertEquals(listOf(lastMonday, lastMonday), repository.requested.takeLast(2))
        }

    @Test
    fun `새 ViewModel 의 첫 복귀 갱신은 init 조회를 중복하지 않는다`() =
        runTest(dispatcher) {
            // 즉시 실패하는 응답이라 init 조회가 ON_RESUME 전에 이미 끝나 있다 — Job 가드가
            // 막지 못하는 창이다. 첫 resume 판단을 화면 저장 상태에 두면 프로세스 사망 뒤
            // «지나갔음» 만 복원돼 새 ViewModel 의 init 과 겹친다 (#736 리뷰).
            val repository = ScriptedRepository(emptyMap())
            val viewModel = start(repository)
            assertEquals(1, repository.requested.size)

            // 프로세스 복원 직후의 첫 ON_RESUME 에 해당한다.
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(1, repository.requested.size)
        }

    @Test
    fun `두 번째 복귀부터는 실패한 주차를 다시 시도한다`() =
        runTest(dispatcher) {
            // 첫 resume 을 건너뛰는 것이 «영영 갱신 안 함» 이 되면 #723 이 되돌아온다.
            val repository = ScriptedRepository(emptyMap())
            val viewModel = start(repository)
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(2, repository.requested.size)
        }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    private class ScriptedRepository(
        var script: Map<LocalDate, Result<WeeklyReport>>,
    ) : WeeklyReportRepository {
        val requested = mutableListOf<LocalDate>()

        override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
            val monday = LocalDate.parse(date)
            requested += monday
            return script[monday] ?: Result.failure(IllegalStateException("timeout"))
        }
    }

    private fun report(diaryAmount: Int = 0) =
        WeeklyReport(
            dailyQuestionAmount = 0,
            diaryAmount = diaryAmount,
            summaryText = "",
            week = emptyList(),
            dailyQuestions = emptyList(),
            emotions = emptyList(),
            emotionAnalysis = EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0),
        )

    @Test
    fun `재진입 갱신은 그 사이 바뀐 프로필 이름을 반영한다`() =
        runTest(dispatcher) {
            // 종전에는 이미 Loaded 이고 같은 주면 `copy(report =)` 로만 갱신해, 방금 받아 온
            // 이름을 버렸다. 설정에서 이름을 바꾸고 돌아와도 주를 옮기기 전까지 옛 이름이
            // 남는다 — 폴링 방출은 첫 조회의 이름을 그대로 싣고 오므로 갈라 둘 이유가 없다 (#1693 리뷰).
            var name = "옛이름"
            val userRepository =
                FakeUserRepository.strict().apply {
                    onReceiverListFlow = { flowOf(emptyList()) }
                    onGetMyProfile = { User(name = name, email = "a@b.c", phone = null, profileImageUrl = null) }
                }
            val repository = ScriptedRepository(mapOf(thisMonday to Result.success(report())))
            val viewModel = start(repository, userRepository)

            assertEquals("옛이름", (viewModel.uiState.value as WeeklyReportUiState.Success).userName)

            // 화면에 들어온 첫 ON_RESUME 은 init 조회가 덮으므로 건너뛴다 (#736 리뷰).
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            // 설정에서 이름을 바꾸고 돌아왔다.
            name = "새이름"
            changeTracker.notifyChanged()
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals("새이름", (viewModel.uiState.value as WeeklyReportUiState.Success).userName)
        }

    private fun TestScope.start(
        repository: WeeklyReportRepository,
        userRepository: FakeUserRepository = userRepository(),
    ): WeeklyReportViewModel {
        val viewModel =
            WeeklyReportViewModel(ObserveWeeklyReportUseCase(repository, userRepository), changeTracker, RecordingErrorReporter())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel
    }

    private fun userRepository(): FakeUserRepository =
        FakeUserRepository.strict().apply {
            onReceiverListFlow = { flowOf(emptyList()) }
            onGetMyProfile = { User(name = "adamtia", email = "a@b.c", phone = null, profileImageUrl = null) }
        }
}
