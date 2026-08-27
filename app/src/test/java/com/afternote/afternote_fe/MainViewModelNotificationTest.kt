package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import com.afternote.afternote_fe.notification.NotificationNavigationRequest
import com.afternote.afternote_fe.notification.NotificationTopLevelDestination
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelNotificationTest {
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
    fun `소비한 occurrence token은 SavedState 복원 뒤 다시 enqueue하지 않는다`() {
        val savedStateHandle = SavedStateHandle()
        val request = request("occurrence-1")
        val firstViewModel = viewModel(savedStateHandle)

        firstViewModel.enqueueNotificationRequest(request)
        firstViewModel.consumeNotificationRequest(request.occurrenceToken)

        val restoredValues =
            savedStateHandle.keys().associateWith { key ->
                savedStateHandle.get<Any?>(key)
            }
        val restoredViewModel = viewModel(SavedStateHandle(restoredValues))
        restoredViewModel.enqueueNotificationRequest(request)

        assertNull(restoredViewModel.pendingNotificationRequest.value)
    }

    @Test
    fun `이전 token 처리 완료는 처리 중 도착한 최신 요청을 지우지 않는다`() {
        val viewModel = viewModel(SavedStateHandle())
        val first = request("occurrence-1")
        val latest = request("occurrence-2")

        viewModel.enqueueNotificationRequest(first)
        viewModel.enqueueNotificationRequest(latest)
        viewModel.consumeNotificationRequest(first.occurrenceToken)

        assertEquals(latest, viewModel.pendingNotificationRequest.value)
    }

    @Test
    fun `현재 pending token을 소비하면 요청을 비운다`() {
        val viewModel = viewModel(SavedStateHandle())
        val request = request("occurrence-1")

        viewModel.enqueueNotificationRequest(request)
        viewModel.consumeNotificationRequest(request.occurrenceToken)

        assertNull(viewModel.pendingNotificationRequest.value)
    }

    private fun viewModel(savedStateHandle: SavedStateHandle): MainViewModel =
        MainViewModel(
            authRepository = FakeAuthRepository(loggedIn = false),
            userRepository = FakeUserRepository.strict(),
            savedStateHandle = savedStateHandle,
        )

    private fun request(occurrenceToken: String): NotificationNavigationRequest =
        NotificationNavigationRequest(
            destination = NotificationTopLevelDestination.TIME_LETTER,
            occurrenceToken = occurrenceToken,
            parameters = emptyMap(),
        )
}
