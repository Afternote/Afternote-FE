package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecipientListViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `재진입 refresh는 수신자 흐름을 새로 구독해 최신 목록을 조회한다`() =
        runTest {
            val repository =
                FakeUserRepository(
                    receivers = listOf(Receiver(1L, "첫 수신자", "친구", "auth-1")),
                )
            val viewModel = RecipientListViewModel(repository)
            backgroundScope.launch { viewModel.recipients.collect() }
            advanceUntilIdle()

            repository.receiverState.value = listOf(Receiver(2L, "새 수신자", "가족", "auth-2"))
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(2, repository.receiverListFlowCalls)
            assertEquals("새 수신자", viewModel.recipients.value.single().name)
        }
}
