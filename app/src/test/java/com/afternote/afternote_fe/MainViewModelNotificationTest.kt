package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.afternote_fe.notification.NotificationEntrySource
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.domain.testing.FakeAuthRepository
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

        firstViewModel.enqueueNotificationEntry(request)
        firstViewModel.consumeNotificationEntry(request.identityKey)

        val restoredValues =
            savedStateHandle.keys().associateWith { key ->
                savedStateHandle.get<Any?>(key)
            }
        val restoredViewModel = viewModel(SavedStateHandle(restoredValues))
        restoredViewModel.enqueueNotificationEntry(request)

        assertNull(restoredViewModel.pendingNotificationEntry.value)
    }

    @Test
    fun `이전 token 처리 완료는 처리 중 도착한 최신 요청을 지우지 않는다`() {
        val viewModel = viewModel(SavedStateHandle())
        val first = request("occurrence-1")
        val latest = request("occurrence-2")

        viewModel.enqueueNotificationEntry(first)
        viewModel.enqueueNotificationEntry(latest)
        viewModel.consumeNotificationEntry(first.identityKey)

        assertEquals(latest, viewModel.pendingNotificationEntry.value)
    }

    @Test
    fun `현재 pending token을 소비하면 요청을 비운다`() {
        val viewModel = viewModel(SavedStateHandle())
        val request = request("occurrence-1")

        viewModel.enqueueNotificationEntry(request)
        viewModel.consumeNotificationEntry(request.identityKey)

        assertNull(viewModel.pendingNotificationEntry.value)
    }

    @Test
    fun `같은 occurrence token도 source가 다르면 별도 요청이다`() {
        val viewModel = viewModel(SavedStateHandle())
        val daily = request("shared-token", NotificationEntrySource.DAILY)
        val fcm = request("shared-token", NotificationEntrySource.FCM)

        viewModel.enqueueNotificationEntry(daily)
        viewModel.consumeNotificationEntry(daily.identityKey)
        viewModel.enqueueNotificationEntry(fcm)

        assertEquals(fcm, viewModel.pendingNotificationEntry.value)
    }

    private fun viewModel(savedStateHandle: SavedStateHandle): MainViewModel =
        MainViewModel(
            authRepository = FakeAuthRepository(loggedIn = false),
            savedStateHandle = savedStateHandle,
        )

    private fun request(
        occurrenceId: String,
        source: NotificationEntrySource = NotificationEntrySource.FCM,
        destination: NotificationDestination = NotificationDestination.HOME,
    ): NotificationEntryRequest =
        NotificationEntryRequest(
            source = source,
            occurrenceId = occurrenceId,
            destination = destination,
        )
}
