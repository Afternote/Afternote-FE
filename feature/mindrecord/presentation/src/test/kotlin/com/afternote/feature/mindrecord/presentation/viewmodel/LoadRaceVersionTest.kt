package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import kotlinx.coroutines.CompletableDeferred
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
 * 조회와 겹친 쓰기를 재조회 가드가 삼키지 않는지 (#736 리뷰).
 *
 * 재조회 가드는 «내가 본 버전» 과 현재 버전을 비교한다. 그 «내가 본 버전» 을 조회가 **끝난**
 * 시점에 읽으면, GET 이 서버 snapshot 을 읽은 뒤 응답이 오는 사이에 성공한 쓰기까지 «이미
 * 봤다» 로 기록된다. 결과에는 그 항목이 없는데 버전만 최신이라, 복귀해도 재조회가 일어나지
 * 않고 방금 저장한 항목이 목록에서 빠진 채 고정된다.
 *
 * 창이 열려 있어야 재현되므로 조회를 붙잡아 그 사이에 쓰기를 끼워 넣는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoadRaceVersionTest {
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
    fun `조회 도중 들어온 쓰기는 복귀 갱신에서 다시 조회된다`() =
        runTest(dispatcher) {
            val changeTracker = MindRecordChangeTracker()
            val repository = FakeDailyQuestionRepository(changeTracker = changeTracker)
            val gate = CompletableDeferred<Unit>()
            var heldOnce = false
            repository.onGetList = { _, _ ->
                // 첫 조회만 붙잡는다 — 그 창에서 쓰기를 끼워 넣는다.
                if (!heldOnce) {
                    heldOnce = true
                    gate.await()
                }
                Result.success(emptyList())
            }
            val viewModel = DailyQuestionListViewModel(repository, changeTracker, RecordingErrorReporter())
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            // 조회가 서버 snapshot 을 읽은 뒤, 응답이 오기 전에 쓰기가 성공한다.
            repository.create(DailyQuestionCreatePayload(content = "창 안에서 저장", isDraft = false, questionId = 1L))
            gate.complete(Unit)
            advanceUntilIdle()
            val afterFirstLoad = repository.listQueries.size

            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals(
                "조회와 겹친 쓰기를 «이미 봤다» 로 기록해 재조회를 건너뛰었다",
                afterFirstLoad + 1,
                repository.listQueries.size,
            )
        }
}
