package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * 쓰기 뒤 복귀 갱신이 실제로 다시 조회하는지 (#520 · #736 · #966 리뷰).
 *
 * 재조회 가드는 «데이터가 안 바뀌었으면 안 부른다» 인데, 그 판정은 data 계층이 쓰기 성공에
 * [MindRecordChangeTracker.notifyChanged] 를 부른다는 전제 위에 선다. 그 배선이 빠지면
 * 가드가 «안 바뀌었다» 로 보고 갱신을 건너뛰어 «작성하고 돌아왔는데 목록이 그대로» 가 된다.
 *
 * androidTest 의 같은 시나리오는 컴파일만 되고 실행되지 않아 이 성질을 지켜 주지 못했다 —
 * 그래서 JVM 쪽에도 둔다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChangeTrackerRefreshTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `쓰기가 있었으면 복귀 갱신이 다시 조회한다`() =
        runTest(dispatcher) {
            val changeTracker = MindRecordChangeTracker()
            val repository = FakeDailyQuestionRepository(changeTracker = changeTracker)
            val viewModel = DailyQuestionListViewModel(repository, changeTracker, RecordingErrorReporter())
            // uiState 는 WhileSubscribed 라 구독자가 없으면 로드가 시작되지 않는다.
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()
            val afterFirstLoad = repository.listQueries.size

            repository.create(DailyQuestionCreatePayload(content = "새 답변", isDraft = false, questionId = 1L))
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(afterFirstLoad + 1, repository.listQueries.size)
        }

    @Test
    fun `쓰기가 없었으면 다시 조회하지 않는다`() =
        runTest(dispatcher) {
            // 같은 가드의 반대 방향 — 탭을 오가도 데이터가 그대로면 부르지 않는다 (#736).
            val changeTracker = MindRecordChangeTracker()
            val repository = FakeDailyQuestionRepository(changeTracker = changeTracker)
            val viewModel = DailyQuestionListViewModel(repository, changeTracker, RecordingErrorReporter())
            // uiState 는 WhileSubscribed 라 구독자가 없으면 로드가 시작되지 않는다.
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()
            val afterFirstLoad = repository.listQueries.size

            repeat(3) { viewModel.refreshOnReturn() }
            advanceUntilIdle()

            assertEquals(afterFirstLoad, repository.listQueries.size)
        }
}
