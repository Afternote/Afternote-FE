package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipientListViewModelTest {
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
    fun `getReceivers 조회 실패는 receiverListFlow 정책과 무관하게 Error 상태가 된다`() =
        runTest(dispatcher) {
            // receiverListFlow는 실패를 삼켜 빈 목록을 내지만(#1099 계약), getReceivers 직접
            // 호출은 예외를 던지는 상황을 재현한다 — 화면은 그 정책과 독립적으로 실패를 봐야 한다.
            val repository =
                FakeUserRepository(
                    receivers = emptyList(),
                    onGetReceivers = { error("network down") },
                )
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            assertEquals(RecipientListUiState.Error, viewModel.uiState.value)
            job.cancel()
        }

    @Test
    fun `조회 성공 시 receiverListFlow의 목록을 그대로 노출한다`() =
        runTest(dispatcher) {
            val repository = FakeUserRepository(receivers = listOf(Receiver(1L, "김수신", "가족", "auth-1")))
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is RecipientListUiState.Success)
            assertEquals(listOf("김수신"), (state as RecipientListUiState.Success).recipients.map { it.name })
            job.cancel()
        }

    @Test
    fun `실패 후 재시도가 성공하면 목록을 복구한다`() =
        runTest(dispatcher) {
            var shouldFail = true
            val repository =
                FakeUserRepository(
                    receivers = listOf(Receiver(1L, "김수신", "가족", "auth-1")),
                    onGetReceivers = {
                        if (shouldFail) error("network down") else listOf(Receiver(1L, "김수신", "가족", "auth-1"))
                    },
                )
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }
            advanceUntilIdle()
            assertEquals(RecipientListUiState.Error, viewModel.uiState.value)

            shouldFail = false
            viewModel.retry()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is RecipientListUiState.Success)
            job.cancel()
        }
}
