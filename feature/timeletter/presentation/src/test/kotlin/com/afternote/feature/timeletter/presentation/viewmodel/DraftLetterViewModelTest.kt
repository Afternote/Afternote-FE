package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DraftLetterViewModelTest {
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
    fun `조회 실패는 빈 목록이 아니라 Error 상태가 된다`() =
        runTest(dispatcher) {
            val viewModel = DraftLetterViewModel(FakeDraftTimeLetterRepository(loadFailure = true))

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is DraftLetterUiState.Error)
        }

    @Test
    fun `조회 실패 후 다시 시도하면 목록을 복구한다`() =
        runTest(dispatcher) {
            val repository = FakeDraftTimeLetterRepository(loadFailure = true)
            val viewModel = DraftLetterViewModel(repository)
            advanceUntilIdle()
            repository.loadFailure = false

            viewModel.loadDrafts()
            advanceUntilIdle()

            val state = viewModel.uiState.value as DraftLetterUiState.Success
            assertEquals(listOf(1L), state.drafts.map { it.id })
        }

    @Test
    fun `선택 삭제 실패 시 기존 목록과 선택을 유지하고 오류를 노출한다`() =
        runTest(dispatcher) {
            val repository = FakeDraftTimeLetterRepository(deleteFailure = true)
            val viewModel = DraftLetterViewModel(repository)
            advanceUntilIdle()
            viewModel.toggleEditMode()
            viewModel.toggleSelection(1L)

            viewModel.deleteSelected()
            advanceUntilIdle()

            val state = viewModel.uiState.value as DraftLetterUiState.Success
            assertEquals(listOf(1L), state.drafts.map { it.id })
            assertEquals(setOf(1L), state.selectedIds)
            assertEquals("임시저장 레터를 삭제할 수 없습니다.", state.errorMessage)
            assertEquals(false, state.isDeleting)
        }
}

private class FakeDraftTimeLetterRepository(
    var loadFailure: Boolean = false,
    var deleteFailure: Boolean = false,
) : TimeLetterRepository {
    private val draft =
        TimeLetter(
            id = 1L,
            title = "임시저장",
            sendAt = null,
            deliveredAt = null,
            status = TimeLetterStatus.DRAFT,
            blocks = emptyList(),
            receiverIds = emptyList(),
        )

    override suspend fun getTemporaryTimeLetters(): TimeLetterList {
        if (loadFailure) error("조회 실패")
        return TimeLetterList(timeLetters = listOf(draft), totalCount = 1)
    }

    override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
        if (deleteFailure) error("삭제 실패")
    }

    override suspend fun getTimeLetters(): TimeLetterList = error("사용하지 않음")

    override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter = error("사용하지 않음")

    override suspend fun createTimeLetter(
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode,
        status: TimeLetterStatus,
        receiverIds: List<Long>,
    ): TimeLetter = error("사용하지 않음")

    override suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode?,
        status: TimeLetterStatus?,
    ): TimeLetter = error("사용하지 않음")

    override suspend fun deleteAllTemporary() = Unit
}
