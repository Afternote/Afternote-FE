package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.User
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
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
import java.lang.reflect.Proxy
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
            deepThoughtAmount = 0,
            summaryText = "",
            week = emptyList(),
            dailyQuestions = emptyList(),
            emotions = emptyList(),
            emotionAnalysis = EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0),
        )

    private fun TestScope.start(repository: WeeklyReportRepository): WeeklyReportViewModel {
        val viewModel = WeeklyReportViewModel(repository, userRepository(), changeTracker)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel
    }

    private fun userRepository(): UserRepository =
        Proxy.newProxyInstance(
            UserRepository::class.java.classLoader,
            arrayOf(UserRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getReceiverListFlow" -> flowOf(emptyList<Any>())
                "getMyProfile" -> User(name = "adamtia", email = "a@b.c", phone = null, profileImageUrl = null)
                else -> error("Unexpected user repository call: ${method.name}")
            }
        } as UserRepository
}
