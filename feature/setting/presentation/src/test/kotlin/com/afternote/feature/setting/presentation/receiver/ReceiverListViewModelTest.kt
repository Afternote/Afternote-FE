package com.afternote.feature.setting.presentation.receiver

import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.Receiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 설정 수신자 목록의 조회 상태 (#1281). 저장소가 내는 [ReceiverListState] 를 화면 상태로 옮기는 규칙만 본다.
 * 저장소 쪽 방출 규칙은 core:data 의 `ReceiverListStateFlowTest` 가 지킨다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val states = MutableSharedFlow<ReceiverListState>()
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
    fun `첫 조회 결과 전에는 행 없는 로딩이다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading), viewModel.uiState.value)

            states.emit(ReceiverListState.Loading(null))

            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading), viewModel.uiState.value)
        }

    @Test
    fun `조회에 성공한 0건만 Ready 다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            states.emit(ReceiverListState.Success(emptyList()))

            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Ready), viewModel.uiState.value)
        }

    @Test
    fun `첫 실패는 행 없는 Failure 이고 다시 시도는 한 번만 다시 조회한다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            states.emit(ReceiverListState.Loading(null))
            states.emit(ReceiverListState.Failure(null))
            viewModel.retry()

            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure), viewModel.uiState.value)
            assertEquals(1, repository.refreshReceiverListCalls)
        }

    @Test
    fun `갱신 실패는 마지막 성공 행을 남기고 다시 시도할 수 있다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            states.emit(ReceiverListState.Success(listOf(KIM, PARK)))
            states.emit(ReceiverListState.Loading(listOf(KIM, PARK)))
            assertEquals(ReceiverListUiState(listOf(KIM_ITEM, PARK_ITEM), ReceiverListLoadState.Loading), viewModel.uiState.value)

            states.emit(ReceiverListState.Failure(listOf(KIM, PARK)))
            viewModel.retry()

            assertEquals(
                ReceiverListUiState(listOf(KIM_ITEM, PARK_ITEM), ReceiverListLoadState.RefreshFailure),
                viewModel.uiState.value,
            )
            assertEquals(1, repository.refreshReceiverListCalls)
        }

    /** 목록 위 배너는 보여 줄 행이 있을 때만 뜻이 있다. 직전 성공이 0건이면 전면 실패 안내가 맡는다. */
    @Test
    fun `직전 성공이 0건인 갱신 실패는 Failure 다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            states.emit(ReceiverListState.Success(emptyList()))
            states.emit(ReceiverListState.Loading(emptyList()))
            states.emit(ReceiverListState.Failure(emptyList()))

            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure), viewModel.uiState.value)
        }

    @Test
    fun `조회 중과 성공 상태에서는 다시 시도가 조회를 더 보내지 않는다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            viewModel.retry()
            states.emit(ReceiverListState.Success(listOf(KIM)))
            viewModel.retry()
            states.emit(ReceiverListState.Loading(listOf(KIM)))
            viewModel.retry()

            assertEquals(0, repository.refreshReceiverListCalls)
        }

    /**
     * 5초를 넘겨 돌아오면 저장소 구독이 새로 시작해 이전 목록 없는 Loading 부터 온다. 그 조회 동안 행을 비우면
     * 목록 자리가 로딩으로 바뀌어 검색·선택·스크롤이 사라진다.
     */
    @Test
    fun `5초를 넘겨 다시 구독해도 새 구독의 첫 조회 동안 보이던 행을 유지한다`() =
        runTest(dispatcher) {
            val viewModel = ReceiverListViewModel(repository)
            val firstVisit = backgroundScope.launch { viewModel.uiState.collect {} }
            states.emit(ReceiverListState.Success(listOf(KIM, PARK)))

            firstVisit.cancel()
            advanceTimeBy(STOP_TIMEOUT_MILLIS + 1)
            assertEquals(0, states.subscriptionCount.value)

            backgroundScope.launch { viewModel.uiState.collect {} }
            states.emit(ReceiverListState.Loading(null))

            assertEquals(ReceiverListUiState(listOf(KIM_ITEM, PARK_ITEM), ReceiverListLoadState.Loading), viewModel.uiState.value)

            states.emit(ReceiverListState.Success(listOf(KIM)))

            assertEquals(ReceiverListUiState(listOf(KIM_ITEM), ReceiverListLoadState.Ready), viewModel.uiState.value)
        }

    @Test
    fun `SignedOut 뒤 로딩은 이전 계정 행을 되살리지 않는다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            states.emit(ReceiverListState.Success(listOf(KIM)))
            states.emit(ReceiverListState.SignedOut)
            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading), viewModel.uiState.value)

            states.emit(ReceiverListState.Loading(null))

            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading), viewModel.uiState.value)
        }

    @Test
    fun `401 로 이전 목록을 버린 실패는 행을 비운다`() =
        runTest(dispatcher) {
            val viewModel = subscribedViewModel()

            states.emit(ReceiverListState.Success(listOf(KIM)))
            states.emit(ReceiverListState.Loading(listOf(KIM)))
            states.emit(ReceiverListState.Failure(null))
            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure), viewModel.uiState.value)

            states.emit(ReceiverListState.Loading(null))

            assertEquals(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading), viewModel.uiState.value)
        }

    private fun TestScope.subscribedViewModel(): ReceiverListViewModel =
        ReceiverListViewModel(repository).also { viewModel ->
            backgroundScope.launch { viewModel.uiState.collect {} }
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        val KIM = Receiver(receiverId = 7L, name = "김수신", relation = "가족", authCode = "")
        val PARK = Receiver(receiverId = 11L, name = "박친구", relation = "친구", authCode = "")
        val KIM_ITEM = ReceiverListItem(receiverId = 7L, name = "김수신", relation = "가족")
        val PARK_ITEM = ReceiverListItem(receiverId = 11L, name = "박친구", relation = "친구")
    }
}
