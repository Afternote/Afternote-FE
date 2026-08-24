package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * 재진입 갱신이 보고 있던 필터를 지키는지 (#702 리뷰 지적).
 *
 * `load()` 는 새 `Success` 를 만들어 상태를 갈아치운다. 종전에는 init·오류 재시도에서만
 * 돌아 «필터가 적용된 Success 위에서 실행될» 경로가 없었는데, 이 PR 의 `refreshOnReturn()`
 * 이 그 경로를 처음 만든다 — 승계하지 않으면 화면 off/on 한 번에 정렬·기간 필터가 조용히
 * 초기화된다. 로딩을 안 보이게 한 것만으로는 «갈아치우지 않는다» 가 되지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverRefreshFilterTest {
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
    fun `재진입 갱신이 정렬과 기간 필터를 지운다면 안 된다`() =
        runTest(dispatcher) {
            val viewModel = ReceiverMindRecordViewModel(repository = FakeReceiverRepository())
            advanceUntilIdle()

            val applied =
                ReceiverMindRecordFilter(
                    sortOrder = SortOrder.OLDEST,
                    fromDate = "2026-08-01",
                    toDate = "2026-08-31",
                )
            viewModel.applyFilter(applied)
            advanceUntilIdle()

            viewModel.refreshOnReturn()
            advanceUntilIdle()

            val state = viewModel.uiState.value as ReceiverMindRecordUiState.Success

            assertEquals("갱신해도 보고 있던 필터가 남는다", applied, state.filter)
        }
}

private class FakeReceiverRepository : MindRecordReceiverRepository {
    override suspend fun getAll(): Result<ReceiverMindRecords> =
        Result.success(
            ReceiverMindRecords(
                dailyQuestions =
                    listOf(
                        MindRecordSummary(
                            id = 1L,
                            type = MindRecordType.DAILY_QUESTION,
                            title = "질문",
                            content = "답변",
                            recordDate = "2026-08-10",
                            isDraft = false,
                            createdAt = "2026.08.10 월",
                        ),
                    ),
                diaries = emptyList(),
            ),
        )
}
