package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.model.user.User
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
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
import java.lang.reflect.Proxy

/**
 * 화면별 요청 수 회귀 가드 (#736).
 *
 * 마음의 기록 첫 진입 한 번에 HTTP 요청이 7건 나갔다 — 화면에 보이는 데일리 질문에
 * 필요한 건 2건이고, 나머지는 **미노출 탭 프리페치**와 **최초 `ON_RESUME` 중복**이었다.
 *
 * 여기서 고정하는 계약: 자동 갱신은 **데이터가 바뀌었을 때만** 다시 부른다. 갱신 자체를
 * 없애면 #520(작성 후 목록 미갱신)이 되돌아오므로, 그 경계를 함께 고정한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MindRecordRequestCountTest {
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
    fun `데일리질문 첫 진입은 조회 2건으로 끝난다`() =
        runTest(dispatcher) {
            // today 1건 + 목록 1건. 최초 ON_RESUME 이 같은 두 건을 다시 내보내면 안 된다.
            val repository = CountingDailyQuestionRepository()
            val viewModel = startDailyQuestion(repository)

            viewModel.refreshOnReturn() // 진입 직후의 ON_RESUME
            advanceUntilIdle()

            assertEquals("today 조회", 1, repository.todayCalls)
            assertEquals("목록 조회", 1, repository.listCalls)
        }

    @Test
    fun `탭을 오가도 데이터가 그대로면 다시 부르지 않는다`() =
        runTest(dispatcher) {
            val repository = CountingDailyQuestionRepository()
            val viewModel = startDailyQuestion(repository)

            repeat(5) { viewModel.refreshOnReturn() }
            advanceUntilIdle()

            assertEquals(1, repository.listCalls)
        }

    @Test
    fun `쓰기가 성공하면 다음 복귀에서 최신 목록을 부른다`() =
        runTest(dispatcher) {
            // #520 회귀 방지 — 작성하고 돌아왔는데 목록이 그대로면 안 된다.
            val repository = CountingDailyQuestionRepository()
            val viewModel = startDailyQuestion(repository)

            changeTracker.notifyChanged() // 작성 화면에서 저장 성공
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(2, repository.listCalls)
        }

    @Test
    fun `한 번 갱신한 뒤 또 돌아오면 다시 부르지 않는다`() =
        runTest(dispatcher) {
            val repository = CountingDailyQuestionRepository()
            val viewModel = startDailyQuestion(repository)

            changeTracker.notifyChanged()
            viewModel.refreshOnReturn()
            advanceUntilIdle()
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals("변경 1회에 갱신 1회", 2, repository.listCalls)
        }

    @Test
    fun `주간리포트도 데이터가 그대로면 복귀에 다시 부르지 않는다`() =
        runTest(dispatcher) {
            val repository = CountingWeeklyReportRepository()
            val viewModel =
                WeeklyReportViewModel(ObserveWeeklyReportUseCase(repository, userRepository()), changeTracker, RecordingErrorReporter())
            backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
            advanceUntilIdle()

            repeat(3) { viewModel.refreshOnReturn() }
            advanceUntilIdle()

            assertEquals("탭을 처음 열 때 1회만", 1, repository.calls)
        }

    @Test
    fun `분석이 끝나지 않았으면 복귀 갱신을 막지 않는다`() =
        runTest(dispatcher) {
            // changeTracker 는 일기·데일리질문의 **쓰기** 성공에서만 올라간다. 감정 분석
            // 완료는 서버가 비동기로 채우는 상태라 그 카운터가 모른다. 폴링(8회 × 8초)이
            // 소진된 뒤 대기가 남으면 복귀 갱신이 유일한 복구 경로인데, «데이터가 그대로면
            // 부르지 않는다» 가드가 그것까지 막으면 «분석 중» 이 앱 재시작까지 굳는다.
            val repository = CountingWeeklyReportRepository(pendingAnalysis = true)
            val viewModel =
                WeeklyReportViewModel(ObserveWeeklyReportUseCase(repository, userRepository()), changeTracker, RecordingErrorReporter())
            backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
            advanceUntilIdle()
            advanceTimeBy(10 * 60 * 1000L)
            advanceUntilIdle()
            // 화면에 들어온 첫 ON_RESUME 은 init 조회가 덮으므로 건너뛴다 — 여기서 보려는
            // 것은 «나갔다 돌아온» 갱신이라 그 뒤부터 센다 (#736 리뷰).
            viewModel.refreshOnReturn()
            advanceUntilIdle()
            val afterPolling = repository.calls

            viewModel.refreshOnReturn()
            advanceUntilIdle()

            // 갱신이 나갔는지만 본다 — 새 로드가 아직 PENDING 이면 폴링도 함께 다시 도는 것이
            // 정상이라 «정확히 1건» 으로 조이면 그 정상 동작이 실패가 된다.
            assertTrue(
                "폴링이 끝나도 복귀 갱신은 통과한다",
                repository.calls > afterPolling,
            )
        }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    private fun TestScope.startDailyQuestion(repository: DailyQuestionRepository): DailyQuestionListViewModel {
        val viewModel = DailyQuestionListViewModel(repository, changeTracker, RecordingErrorReporter())
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect { } }
        advanceUntilIdle()
        return viewModel
    }

    private class CountingDailyQuestionRepository : DailyQuestionRepository {
        var todayCalls = 0
        var listCalls = 0

        override suspend fun getList(
            date: String?,
            draftOnly: Boolean?,
        ): Result<List<DailyQuestion>> {
            listCalls++
            return Result.success(emptyList())
        }

        override suspend fun getToday(): Result<TodayDailyQuestion> {
            todayCalls++
            return Result.success(
                TodayDailyQuestion(questionId = 1L, day = 1, content = "질문", isAnswered = false),
            )
        }

        override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> = error("호출되면 안 됨")

        override suspend fun update(
            id: Long,
            payload: DailyQuestionUpdatePayload,
        ): Result<Long> = error("호출되면 안 됨")

        override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
    }

    private class CountingWeeklyReportRepository(
        private val pendingAnalysis: Boolean = false,
    ) : WeeklyReportRepository {
        var calls = 0

        override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
            calls++
            return Result.success(
                WeeklyReport(
                    dailyQuestionAmount = 0,
                    diaryAmount = 0,
                    summaryText = "",
                    week = emptyList(),
                    dailyQuestions = emptyList(),
                    emotions = emptyList(),
                    emotionAnalysis =
                        if (pendingAnalysis) {
                            EmotionAnalysis(total = 1, succeeded = 0, pending = 1, failed = 0)
                        } else {
                            EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0)
                        },
                ),
            )
        }
    }

    private fun userRepository(): MyProfileRepository =
        Proxy.newProxyInstance(
            MyProfileRepository::class.java.classLoader,
            arrayOf(MyProfileRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getReceiverListFlow" -> flowOf(emptyList<Any>())
                "getMyProfile" -> User(name = "adamtia", email = "a@b.c", phone = null, profileImageUrl = null)
                else -> error("Unexpected user repository call: ${method.name}")
            }
        } as MyProfileRepository
}
