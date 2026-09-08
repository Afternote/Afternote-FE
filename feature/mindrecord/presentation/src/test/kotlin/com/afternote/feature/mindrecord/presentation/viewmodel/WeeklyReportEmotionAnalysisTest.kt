package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportEmotion
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 감정 분석 하위 상태의 화면 전이 가드 (#725).
 *
 * 주간리포트 요청 자체는 HTTP 200 이라, 분석 대기·실패가 `emotions=[]` 로만 도착한다.
 * 종전에는 그 빈 목록을 "키워드 0건" 으로 확정해 정상 리포트처럼 보여줬다.
 *
 * 여기서 고정하는 전이: **대기→완료, 대기→실패, 실패→재시도, 실제 빈 결과**.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyReportEmotionAnalysisTest {
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
    fun `분석 대기는 키워드 0건으로 확정되지 않는다`() =
        runTest(dispatcher) {
            // 실서버 실측(2026-08-23) 형태 — 일기 1건 저장 직후 pending 이 유지되고 emotions 는 [].
            val viewModel = viewModel(listOf(report(total = 1, pending = 1)))
            collectUiState(viewModel)
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success
            assertTrue(state.emotionKeywords.isEmpty())
            assertEquals(EmotionAnalysisStatus.PENDING, state.emotionAnalysisStatus)
        }

    @Test
    fun `대기에서 완료로 바뀌면 머무른 화면에 결과가 반영된다`() =
        runTest(dispatcher) {
            // 분석 완료를 알려주는 채널이 없어 제한된 재조회로 기다린다 — 그 사이 화면을 떠나지 않는다.
            val viewModel =
                viewModel(
                    listOf(
                        report(total = 1, pending = 1),
                        report(total = 1, succeeded = 1, emotions = listOf(WeeklyReportEmotion("평온", 100))),
                    ),
                )
            collectUiState(viewModel)
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success
            assertEquals(EmotionAnalysisStatus.COMPLETED, state.emotionAnalysisStatus)
            assertEquals(listOf("평온"), state.emotionKeywords.map { it.keyword })
        }

    @Test
    fun `대기에서 실패로 바뀌면 정상 빈 상태로 덮지 않는다`() =
        runTest(dispatcher) {
            val viewModel =
                viewModel(
                    listOf(
                        report(total = 2, pending = 2),
                        report(total = 2, failed = 2),
                    ),
                )
            collectUiState(viewModel)
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success
            assertEquals(EmotionAnalysisStatus.FAILED, state.emotionAnalysisStatus)
        }

    @Test
    fun `실패 상태에서 재시도하면 다시 조회해 완료로 바뀐다`() =
        runTest(dispatcher) {
            val viewModel =
                viewModel(
                    listOf(
                        report(total = 1, failed = 1),
                        report(total = 1, succeeded = 1, emotions = listOf(WeeklyReportEmotion("감사", 100))),
                    ),
                )
            collectUiState(viewModel)
            advanceUntilIdle()
            assertEquals(
                EmotionAnalysisStatus.FAILED,
                (viewModel.uiState.value as WeeklyReportUiState.Success).emotionAnalysisStatus,
            )

            viewModel.retryEmotionAnalysis()
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success
            assertEquals(EmotionAnalysisStatus.COMPLETED, state.emotionAnalysisStatus)
            assertEquals(listOf("감사"), state.emotionKeywords.map { it.keyword })
        }

    @Test
    fun `분석 대상이 없으면 그 자리에서 완료된 빈 결과다`() =
        runTest(dispatcher) {
            // 기록이 하나도 없는 주 — 이때의 0건만 "실제로 0건" 이다.
            val viewModel = viewModel(listOf(report(total = 0)))
            collectUiState(viewModel)
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success
            assertEquals(EmotionAnalysisStatus.NOTHING_TO_ANALYZE, state.emotionAnalysisStatus)
        }

    @Test
    fun `대기가 끝나지 않아도 무한히 재조회하지 않는다`() =
        runTest(dispatcher) {
            // 실측처럼 계속 pending 인 경우 — 폴링이 멈추고 화면은 대기 상태로 남는다.
            var calls = 0
            // fake 는 마지막 응답을 반복하므로 한 건이면 충분하다.
            val viewModel = viewModel(listOf(report(total = 1, pending = 1))) { calls++ }
            collectUiState(viewModel)
            advanceUntilIdle()
            val callsAfterPolling = calls

            advanceTimeBy(10 * 60 * 1000L)
            advanceUntilIdle()

            assertEquals("폴링이 끝난 뒤에는 더 조회하지 않는다", callsAfterPolling, calls)
            // 결정적인 값이다 — 최초 1회 + 폴링 8회. 범위로 두면 폴링을 3회로 줄이거나
            // 11회로 늘려도 통과해 회귀를 못 잡는다.
            assertEquals("최초 조회 1회 + 폴링 8회", 9, calls)
        }

    @Test
    fun `성공분 키워드가 이미 있어도 대기 중이면 대기로 알린다`() =
        runTest(dispatcher) {
            // 부분 성공에서는 완료분 키워드가 emotions 에 실려 내려온다(BE buildTopEmotions 에
            // 완료 게이트 없음). 키워드 유무를 상태보다 먼저 보면 폴백 요약이 최종처럼 확정된다.
            val viewModel =
                viewModel(
                    listOf(
                        report(
                            total = 2,
                            succeeded = 1,
                            pending = 1,
                            emotions = listOf(WeeklyReportEmotion(keyword = "가족", percentage = 60)),
                        ),
                    ),
                ) {}
            collectUiState(viewModel)
            advanceUntilIdle()

            val state = viewModel.uiState.value as WeeklyReportUiState.Success

            assertTrue("성공분 키워드는 그대로 보여준다", state.emotionKeywords.isNotEmpty())
            assertEquals(EmotionAnalysisStatus.PENDING, state.emotionAnalysisStatus)
        }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    /**
     * `uiState` 는 `WhileSubscribed` 라 구독자가 없으면 `Loading` 에 머문다.
     * 화면이 떠 있는 상황을 재현하려면 수집을 켜 둬야 한다.
     */
    private fun TestScope.collectUiState(viewModel: WeeklyReportViewModel) {
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
    }

    private fun report(
        total: Int,
        succeeded: Int = 0,
        pending: Int = 0,
        failed: Int = 0,
        emotions: List<WeeklyReportEmotion> = emptyList(),
    ) = WeeklyReport(
        dailyQuestionAmount = 0,
        diaryAmount = if (total > 0) 1 else 0,
        summaryText = "이번 주 기록을 바탕으로 인사이트를 준비 중이에요.",
        week = emptyList(),
        dailyQuestions = emptyList(),
        emotions = emotions,
        emotionAnalysis =
            EmotionAnalysis(total = total, succeeded = succeeded, pending = pending, failed = failed),
    )

    /** [responses] 를 순서대로 돌려주고, 다 쓰면 마지막 응답을 계속 준다. */
    private fun viewModel(
        responses: List<WeeklyReport>,
        onCall: () -> Unit = {},
    ): WeeklyReportViewModel {
        var index = 0
        val repository =
            object : WeeklyReportRepository {
                override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
                    onCall()
                    val report = responses[minOf(index, responses.lastIndex)]
                    index++
                    return Result.success(report)
                }
            }
        return WeeklyReportViewModel(ObserveWeeklyReportUseCase(repository, userRepository()), changeTracker, RecordingErrorReporter())
    }

    private fun userRepository(): FakeUserRepository =
        FakeUserRepository.strict().apply {
            onReceiverListFlow = { flowOf(emptyList()) }
            onGetMyProfile = { User(name = "adamtia", email = "a@b.c", phone = null, profileImageUrl = null) }
        }
}
