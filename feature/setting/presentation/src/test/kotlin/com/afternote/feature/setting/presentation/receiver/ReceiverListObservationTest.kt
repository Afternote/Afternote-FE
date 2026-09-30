package com.afternote.feature.setting.presentation.receiver

import androidx.lifecycle.ViewModelStore
import com.afternote.core.domain.testing.FakeUserReceiverRepository
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
class ReceiverListObservationTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val source = MutableStateFlow(listOf(Receiver(37L, "수신자", "가족")))
    private var starts = 0
    private var stops = 0

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
    fun `활성화 전에는 구독하지 않고 중복 활성화도 구독 하나만 만든다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            runCurrent()
            assertEquals(0, starts)

            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()

            assertEquals(1, starts)
            assertEquals(
                listOf(37L),
                viewModel.uiState.value.receivers
                    .map { it.receiverId },
            )
        }

    @Test
    fun `5초 안에 돌아오면 기존 구독과 목록을 유지한다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(ReceiverListIntent.ObservationStopped)
            runCurrent()
            advanceTimeBy(4_999)
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            advanceTimeBy(5_001)
            runCurrent()

            assertEquals(1, starts)
            assertEquals(0, stops)
            assertEquals(
                listOf(37L),
                viewModel.uiState.value.receivers
                    .map { it.receiverId },
            )
        }

    @Test
    fun `5초 뒤 구독은 끝나지만 목록은 남고 재진입에서 최신 목록을 받는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(ReceiverListIntent.ObservationStopped)
            runCurrent()
            advanceTimeBy(5_000)
            runCurrent()
            source.value = listOf(Receiver(91L, "다른 수신자", "친구"))
            runCurrent()

            assertEquals(1, stops)
            assertEquals(
                listOf(37L),
                viewModel.uiState.value.receivers
                    .map { it.receiverId },
            )
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            assertEquals(2, starts)
            assertEquals(
                listOf(91L),
                viewModel.uiState.value.receivers
                    .map { it.receiverId },
            )
        }

    @Test
    fun `저장소가 세션 경계에서 빈 목록을 내면 이전 목록을 지운다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.onIntent(ReceiverListIntent.ObservationStarted)
            runCurrent()
            source.value = emptyList()
            runCurrent()

            assertEquals(emptyList<Receiver>(), viewModel.uiState.value.receivers)
        }

    private fun viewModel(): ReceiverListViewModel {
        val repository =
            FakeUserReceiverRepository(
                onReceiverListFlow = {
                    flow {
                        starts++
                        try {
                            emitAll(source)
                        } finally {
                            stops++
                        }
                    }
                },
            )
        return ReceiverListViewModel(repository).also { store.put("receiver-list", it) }
    }
}
