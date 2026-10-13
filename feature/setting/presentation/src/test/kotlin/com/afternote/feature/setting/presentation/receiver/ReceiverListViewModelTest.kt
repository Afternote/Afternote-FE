package com.afternote.feature.setting.presentation.receiver

import androidx.lifecycle.ViewModelStore
import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.user.Receiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** 공개 Intent와 UiState를 통해 조회 결과, 재구독, 재시도와 세션 격리를 검증한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()
    private val states = MutableSharedFlow<ReceiverListState>()
    private val repository = FakeUserReceiverRepository(onReceiverListStateFlow = { states })

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `첫 결과 전에는 행 없는 로딩이고 성공한 0건만 Ready 다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            assertState(vm, ReceiverListLoadState.Loading)
            states.emit(ReceiverListState.Loading(null, SESSION_A))
            assertState(vm, ReceiverListLoadState.Loading)
            states.emit(ReceiverListState.Success(emptyList()))
            assertState(vm, ReceiverListLoadState.Ready)
        }

    @Test
    fun `첫 실패 뒤 연속 Retry는 요청 하나만 보내고 즉시 로딩이 된다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            states.emit(ReceiverListState.Loading(null, SESSION_A))
            states.emit(ReceiverListState.Failure(null, discardPrevious = false))
            assertState(vm, ReceiverListLoadState.Failure)
            vm.onIntent(ReceiverListIntent.Retry)
            vm.onIntent(ReceiverListIntent.Retry)
            assertEquals(1, repository.refreshReceiverListCalls)
            assertState(vm, ReceiverListLoadState.Loading)
        }

    @Test
    fun `갱신 실패는 마지막 행을 보존하고 Retry 뒤에도 행을 유지한다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            succeed(KIM, PARK)
            states.emit(ReceiverListState.Loading(listOf(KIM, PARK), SESSION_A))
            assertState(vm, ReceiverListLoadState.Loading, KIM, PARK)
            states.emit(ReceiverListState.Failure(listOf(KIM, PARK), discardPrevious = false))
            assertState(vm, ReceiverListLoadState.RefreshFailure, KIM, PARK)
            vm.onIntent(ReceiverListIntent.Retry)
            assertEquals(1, repository.refreshReceiverListCalls)
            assertState(vm, ReceiverListLoadState.Loading, KIM, PARK)
        }

    @Test
    fun `직전 성공이 0건이면 갱신 실패는 전면 실패다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            succeed()
            states.emit(ReceiverListState.Failure(emptyList(), discardPrevious = false))
            assertState(vm, ReceiverListLoadState.Failure)
        }

    @Test
    fun `로딩과 성공에서는 Retry가 요청을 보내지 않는다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(ReceiverListIntent.Retry)
            succeed(KIM)
            vm.onIntent(ReceiverListIntent.Retry)
            states.emit(ReceiverListState.Loading(listOf(KIM), SESSION_A))
            vm.onIntent(ReceiverListIntent.Retry)
            assertEquals(0, repository.refreshReceiverListCalls)
        }

    @Test
    fun `5초 넘긴 재구독도 같은 세션이면 첫 로딩과 실패에서 행을 유지한다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            succeed(KIM, PARK)
            vm.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(5_001)
            runCurrent()
            assertEquals(0, states.subscriptionCount.value)
            vm.onIntent(ReceiverListIntent.ObservationStarted)
            states.emit(ReceiverListState.Loading(null, SESSION_A))
            assertState(vm, ReceiverListLoadState.Loading, KIM, PARK)
            states.emit(ReceiverListState.Failure(null, discardPrevious = false))
            assertState(vm, ReceiverListLoadState.RefreshFailure, KIM, PARK)
            states.emit(ReceiverListState.Success(listOf(KIM)))
            assertState(vm, ReceiverListLoadState.Ready, KIM)
        }

    @Test
    fun `5초 넘게 중지한 사이 세션이 바뀌면 새 로딩부터 이전 계정 행을 버린다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            succeed(KIM)
            vm.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(5_001)
            runCurrent()
            vm.onIntent(ReceiverListIntent.ObservationStarted)
            states.emit(ReceiverListState.Loading(null, SESSION_B))
            assertState(vm, ReceiverListLoadState.Loading)
            states.emit(ReceiverListState.Failure(null, discardPrevious = false))
            assertState(vm, ReceiverListLoadState.Failure)
        }

    @Test
    fun `SignedOut 뒤 로딩과 실패는 이전 계정 행을 복원하지 않는다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            succeed(KIM)
            states.emit(ReceiverListState.SignedOut)
            assertState(vm, ReceiverListLoadState.Loading)
            states.emit(ReceiverListState.Loading(null, SESSION_B))
            states.emit(ReceiverListState.Failure(null, discardPrevious = false))
            assertState(vm, ReceiverListLoadState.Failure)
        }

    @Test
    fun `동일 구독의 401은 이전 행을 버리고 이후 실패에서도 복원하지 않는다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            succeed(KIM)
            states.emit(ReceiverListState.Loading(listOf(KIM), SESSION_A))
            states.emit(ReceiverListState.Failure(null, discardPrevious = true))
            assertState(vm, ReceiverListLoadState.Failure)
            states.emit(ReceiverListState.Loading(null, SESSION_A))
            states.emit(ReceiverListState.Failure(null, discardPrevious = false))
            assertState(vm, ReceiverListLoadState.Failure)
        }

    @Test
    fun `재구독 첫 요청의 401도 보이던 행을 버린다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            succeed(KIM)
            vm.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(5_001)
            runCurrent()
            vm.onIntent(ReceiverListIntent.ObservationStarted)
            states.emit(ReceiverListState.Loading(null, SESSION_A))
            states.emit(ReceiverListState.Failure(null, discardPrevious = true))
            assertState(vm, ReceiverListLoadState.Failure)
        }

    @Test
    fun `관측이 종료된 실패 화면의 Retry는 요청을 보내지 않는다`() =
        runTest(dispatcher) {
            val vm = viewModel()
            states.emit(ReceiverListState.Loading(null, SESSION_A))
            states.emit(ReceiverListState.Failure(null, discardPrevious = false))
            vm.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(5_001)
            runCurrent()
            vm.onIntent(ReceiverListIntent.Retry)
            assertEquals(0, repository.refreshReceiverListCalls)
        }

    private fun viewModel(): ReceiverListViewModel =
        ReceiverListViewModel(repository).also {
            store.put("receiver-list", it)
            it.onIntent(ReceiverListIntent.ObservationStarted)
        }

    private suspend fun succeed(vararg receivers: Receiver) {
        states.emit(ReceiverListState.Loading(null, SESSION_A))
        states.emit(ReceiverListState.Success(receivers.toList()))
    }

    private fun assertState(
        vm: ReceiverListViewModel,
        loadState: ReceiverListLoadState,
        vararg receivers: Receiver,
    ) {
        assertEquals(loadState, vm.uiState.value.loadState)
        assertEquals(
            receivers.map { it.receiverId },
            vm.uiState.value.receivers
                .map { it.receiverId },
        )
    }

    private companion object {
        const val SESSION_A = "session-a"
        const val SESSION_B = "session-b"
        val KIM = Receiver(receiverId = 7L, name = "김수신", relation = "가족")
        val PARK = Receiver(receiverId = 11L, name = "박친구", relation = "친구")
    }
}
