package com.afternote.feature.receiver.presentation.recordsbox

import com.afternote.feature.receiver.domain.model.SenderEntry
import com.afternote.feature.receiver.domain.testing.FakeSenderRegistryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceivedRecordsViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `저장소가 복원한 발신자 카드를 화면 상태로 노출한다`() =
        runTest(mainDispatcher) {
            val restored = SenderEntry(id = "sender-id", name = "아버지", masterKey = "master-key")
            val viewModel =
                ReceivedRecordsViewModel(
                    SenderRegistry(FakeSenderRegistryRepository(initialSenders = listOf(restored))),
                )

            // WhileSubscribed stateIn의 raw Flow 구독은 StandardTestDispatcher로는
            // backgroundScope에서 즉시 돌지 않으므로 Unconfined로 시작한다.
            backgroundScope.launch(mainDispatcher) { viewModel.senders.collect() }

            assertEquals(listOf(restored), viewModel.senders.value)
        }
}
