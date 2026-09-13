package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModelStore
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.Receiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiverListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val receivers = MutableStateFlow(listOf(Receiver(7L, "김수신", "가족", "auth-7")))
    private var subscriptions = 0
    private var cancellations = 0

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
    fun `화면이 시작하기 전에는 빈 상태를 유지하고 조회하지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            runCurrent()

            assertEquals(emptyList<ReceiverListItem>(), viewModel.uiState.value.receivers)
            assertEquals(0, subscriptions)

            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()

            assertEquals(listOf(ReceiverListItem(7L, "김수신", "가족")), viewModel.uiState.value.receivers)
            assertEquals(1, subscriptions)
        }

    @Test
    fun `화면 시작 신호가 중복되어도 구독은 하나다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()

            assertEquals(1, subscriptions)
            assertEquals(0, cancellations)
        }

    @Test
    fun `5초 이내 복귀는 기존 구독과 최신 목록을 유지한다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(4_999)
            receivers.value = listOf(Receiver(8L, "박수신", "친구", "auth-8"))
            runCurrent()

            assertEquals(listOf(ReceiverListItem(8L, "박수신", "친구")), viewModel.uiState.value.receivers)
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            advanceTimeBy(5_000)
            runCurrent()

            assertEquals(1, subscriptions)
            assertEquals(0, cancellations)
        }

    @Test
    fun `5초 뒤 구독을 중단하고 복귀하면 새 목록을 다시 조회한다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(5_000)
            runCurrent()

            assertEquals(1, cancellations)
            receivers.value = listOf(Receiver(9L, "이수신", "동료", "auth-9"))
            runCurrent()
            assertEquals(listOf(ReceiverListItem(7L, "김수신", "가족")), viewModel.uiState.value.receivers)

            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()

            assertEquals(2, subscriptions)
            assertEquals(listOf(ReceiverListItem(9L, "이수신", "동료")), viewModel.uiState.value.receivers)
        }

    @Test
    fun `중복 중단 신호가 구독 중단 시점을 늦추지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(2_000)
            viewModel.onIntent(ReceiverListIntent.ObservationStopped)
            advanceTimeBy(3_000)
            runCurrent()

            assertEquals(1, cancellations)
        }

    @Test
    fun `저장소가 빈 목록을 내면 이전 수신자를 지운다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()

            receivers.value = emptyList()
            runCurrent()

            assertEquals(emptyList<ReceiverListItem>(), viewModel.uiState.value.receivers)
            assertEquals(1, subscriptions)
        }

    @Test
    fun `ViewModel 제거는 중단 유예 중에도 구독을 즉시 취소한다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(ReceiverListIntent.ObservationStopped)

            store.clear()
            runCurrent()

            assertEquals(1, cancellations)
        }

    private fun viewModel(): ReceiverListViewModel {
        val repository =
            FakeUserReceiverRepository.strict().apply {
                onReceiverListFlow = {
                    flow {
                        subscriptions++
                        try {
                            emitAll(receivers)
                        } finally {
                            cancellations++
                        }
                    }
                }
            }
        return ReceiverListViewModel(repository).also { store.put("receiver-list", it) }
    }
}
