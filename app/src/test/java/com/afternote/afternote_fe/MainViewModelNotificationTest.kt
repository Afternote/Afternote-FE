package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.afternote_fe.notification.NotificationEntrySource
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.domain.repository.notification.NotificationRepository
import com.afternote.core.domain.repository.notification.NotificationTarget
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakePendingReceiverInvitationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `서버 ID가 없는 알림은 서버에 묻지 않고 실어 온 목적지로 연다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationRepository()
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution =
                viewModel.resolveNotificationEntry(
                    request("occurrence-1", destination = NotificationDestination.TIME_LETTER),
                )
            advanceUntilIdle()

            assertEquals(NotificationDestination.TIME_LETTER, resolution.destination)
            assertTrue(repository.targetRequests.isEmpty())
            assertTrue(repository.readRequests.isEmpty())
        }

    @Test
    fun `서버 알림은 탭한 시점의 목적지로 열고 읽음 처리한다`() =
        runTest(dispatcher) {
            val repository =
                FakeNotificationRepository(
                    target = { Result.success(NotificationTarget("RECORD_DETAIL", deleted = false, toast = null)) },
                )
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution = viewModel.resolveNotificationEntry(request("notification:42", serverNotificationId = 42L))
            advanceUntilIdle()

            assertEquals(NotificationDestination.MIND_RECORD, resolution.destination)
            assertFalse(resolution.deleted)
            assertEquals(listOf(42L), repository.targetRequests)
            assertEquals(listOf(42L), repository.readRequests)
        }

    @Test
    fun `지워진 대상은 서버가 준 목록과 안내 문구를 돌려준다`() =
        runTest(dispatcher) {
            val repository =
                FakeNotificationRepository(
                    target = {
                        Result.success(NotificationTarget("TIME_LETTER_LIST", deleted = true, toast = "삭제된 항목이에요"))
                    },
                )
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution = viewModel.resolveNotificationEntry(request("notification:7", serverNotificationId = 7L))

            assertEquals(NotificationDestination.TIME_LETTER, resolution.destination)
            assertTrue(resolution.deleted)
            assertEquals("삭제된 항목이에요", resolution.serverMessage)
        }

    @Test
    fun `목적지 조회가 실패하면 알림이 실어 온 목적지로 연다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationRepository(target = { Result.failure(IllegalStateException("offline")) })
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution =
                viewModel.resolveNotificationEntry(
                    request("notification:1", NotificationEntrySource.FCM, NotificationDestination.TIME_LETTER, 1L),
                )
            advanceUntilIdle()

            assertEquals(NotificationDestination.TIME_LETTER, resolution.destination)
            assertFalse(resolution.deleted)
            assertEquals(listOf(1L), repository.readRequests)
        }

    @Test
    fun `목적지 조회가 응답하지 않으면 기다리지 않고 실어 온 목적지로 연다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationRepository(target = { awaitCancellation() })
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution =
                viewModel.resolveNotificationEntry(
                    request("notification:1", NotificationEntrySource.FCM, NotificationDestination.MIND_RECORD, 1L),
                )

            assertEquals(NotificationDestination.MIND_RECORD, resolution.destination)
        }

    private fun viewModel(
        savedStateHandle: SavedStateHandle,
        notificationRepository: NotificationRepository = FakeNotificationRepository(),
    ): MainViewModel =
        MainViewModel(
            authRepository = FakeAuthRepository(loggedIn = false),
            pendingInvitationStore = FakePendingReceiverInvitationStore(),
            notificationRepository = notificationRepository,
            savedStateHandle = savedStateHandle,
        )

    private fun request(
        occurrenceId: String,
        source: NotificationEntrySource = NotificationEntrySource.FCM,
        destination: NotificationDestination = NotificationDestination.HOME,
        serverNotificationId: Long? = null,
    ): NotificationEntryRequest =
        NotificationEntryRequest(
            source = source,
            occurrenceId = occurrenceId,
            destination = destination,
            serverNotificationId = serverNotificationId,
        )

    private class FakeNotificationRepository(
        private val target: suspend () -> Result<NotificationTarget> = { error("target 은 이 테스트에서 호출되면 안 됨") },
    ) : NotificationRepository {
        val targetRequests = mutableListOf<Long>()
        val readRequests = mutableListOf<Long>()

        override suspend fun target(notificationId: Long): Result<NotificationTarget> {
            targetRequests += notificationId
            return target()
        }

        override suspend fun markRead(notificationId: Long): Result<Unit> {
            readRequests += notificationId
            return Result.success(Unit)
        }
    }
}
