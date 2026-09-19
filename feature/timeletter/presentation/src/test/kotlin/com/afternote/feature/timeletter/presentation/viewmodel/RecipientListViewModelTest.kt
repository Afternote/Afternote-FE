package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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

    @Test
    fun `probe 성공과 flow 구독 사이에 로그아웃되면 빈 목록으로 갱신된다`() =
        runTest(dispatcher) {
            // getReceivers() 직접 호출(probe)이 이전 계정 목록으로 성공한 뒤, receiverListFlow 를
            // 구독하는 시점엔 이미 로그아웃돼 있는 경합을 흉내 낸다. 전에는 receiverListFlow 의 첫
            // 방출을 무조건 버려서 이 경우 세션 종료 신호(빈 목록)까지 삼켜져 화면이 이전 계정 목록에
            // 멈춰 있었다 — 세션 변경은 항상 반영해야 한다.
            val staleReceivers = listOf(Receiver(1L, "이전 계정 수신인", "가족", "auth-1"))
            val repository =
                FakeUserRepository(
                    receivers = staleReceivers,
                    onGetReceivers = { staleReceivers },
                    onReceiverListFlow = { flowOf(emptyList()) },
                )
            val viewModel = RecipientListViewModel(repository)
            val job = launch { viewModel.uiState.collect {} }

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is RecipientListUiState.Success)
            assertEquals(emptyList<String>(), (state as RecipientListUiState.Success).recipients.map { it.name })
            job.cancel()
        }
}
