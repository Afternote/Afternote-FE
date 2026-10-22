package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.user.Receiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class RecipientListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val receiver = Receiver(1L, "김수신", "가족")
    private val states = MutableStateFlow<ReceiverListState>(ReceiverListState.Loading(null, SESSION))
    private val repository = FakeUserReceiverRepository(onReceiverListStateFlow = { states })

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `첫 조회 실패는 빈 목록이 아니라 Error 상태가 된다`() =
        runTest(dispatcher) {
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            assertEquals(RecipientListUiState.Loading, viewModel.uiState.value)

            states.value = ReceiverListState.Failure(previousReceivers = null, discardPrevious = false)
            advanceUntilIdle()

            assertEquals(RecipientListUiState.Error, viewModel.uiState.value)
            job.cancel()
        }

    @Test
    fun `조회 성공 시 목록을 그대로 노출한다`() =
        runTest(dispatcher) {
            states.value = ReceiverListState.Success(listOf(receiver))
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            assertEquals(listOf("김수신"), viewModel.uiState.value.recipientNames())
            job.cancel()
        }

    @Test
    fun `재시도는 저장소 갱신을 요청하고 성공하면 목록을 복구한다`() =
        runTest(dispatcher) {
            states.value = ReceiverListState.Failure(previousReceivers = null, discardPrevious = false)
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            assertEquals(RecipientListUiState.Error, viewModel.uiState.value)

            viewModel.retry()
            states.value = ReceiverListState.Success(listOf(receiver))
            advanceUntilIdle()

            assertEquals(1, repository.refreshReceiverListCalls)
            assertEquals(listOf("김수신"), viewModel.uiState.value.recipientNames())
            job.cancel()
        }

    @Test
    fun `갱신 로딩과 갱신 실패는 마지막 성공 목록을 유지한다`() =
        runTest(dispatcher) {
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            states.value = ReceiverListState.Loading(listOf(receiver), SESSION)
            advanceUntilIdle()
            assertEquals(listOf("김수신"), viewModel.uiState.value.recipientNames())

            states.value = ReceiverListState.Failure(listOf(receiver), discardPrevious = false)
            advanceUntilIdle()
            assertEquals(listOf("김수신"), viewModel.uiState.value.recipientNames())
            job.cancel()
        }

    @Test
    fun `401 로 버린 목록은 유지하지 않고 Error 를 보인다`() =
        runTest(dispatcher) {
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            states.value = ReceiverListState.Failure(listOf(receiver), discardPrevious = true)
            advanceUntilIdle()

            assertEquals(RecipientListUiState.Error, viewModel.uiState.value)
            job.cancel()
        }

    @Test
    fun `세션이 끝나면 이전 계정 목록을 비운다`() =
        runTest(dispatcher) {
            states.value = ReceiverListState.Success(listOf(receiver))
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()

            states.value = ReceiverListState.SignedOut
            advanceUntilIdle()

            assertEquals(emptyList<String>(), viewModel.uiState.value.recipientNames())
            job.cancel()
        }

    private fun RecipientListUiState.recipientNames(): List<String>? =
        (this as? RecipientListUiState.Success)?.recipients?.map { it.name }

    private companion object {
        const val SESSION = "session-a"
    }
}
