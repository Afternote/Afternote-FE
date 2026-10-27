package com.afternote.feature.home.presentation

import com.afternote.core.domain.repository.notification.NotificationEntry
import com.afternote.core.domain.repository.notification.NotificationInbox
import com.afternote.core.domain.repository.notification.NotificationInboxItem
import com.afternote.core.domain.repository.notification.NotificationRepository
import com.afternote.core.domain.repository.notification.NotificationTarget
import com.afternote.feature.home.presentation.notification.NotificationBadgeIntent
import com.afternote.feature.home.presentation.notification.NotificationBadgeViewModel
import com.afternote.feature.home.presentation.notification.NotificationInboxIntent
import com.afternote.feature.home.presentation.notification.NotificationInboxViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 알림함과 홈 탑바 미읽음 점의 회귀 기준 (#2321). */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationInboxViewModelTest {
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
    fun `알림함을 불러오고 누른 항목을 읽음으로 바꾼다`() =
        runTest(dispatcher) {
            val viewModel = NotificationInboxViewModel(FakeInboxRepository(Result.success(inbox(unread = 1))))
            advanceUntilIdle()

            assertEquals(
                listOf(1L, 2L),
                viewModel.uiState.value.items
                    .map { it.id },
            )

            viewModel.onIntent(NotificationInboxIntent.Open(1L))
            assertTrue(
                viewModel.uiState.value.items
                    .all { it.isRead },
            )
        }

    @Test
    fun `다시 불러오다 실패해도 보던 목록을 지우지 않는다`() =
        runTest(dispatcher) {
            val repository = FakeInboxRepository(Result.success(inbox(unread = 1)))
            val viewModel = NotificationInboxViewModel(repository)
            advanceUntilIdle()

            repository.result = Result.failure(IllegalStateException("offline"))
            viewModel.onIntent(NotificationInboxIntent.Refresh)
            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.items.size)
            assertFalse(viewModel.uiState.value.loadFailed)
        }

    @Test
    fun `미읽음이 있을 때만 점을 찍는다`() =
        runTest(dispatcher) {
            val repository = FakeInboxRepository(Result.success(inbox(unread = 3)))
            val viewModel = NotificationBadgeViewModel(repository)

            viewModel.onIntent(NotificationBadgeIntent.Refresh)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasUnread)

            repository.result = Result.success(inbox(unread = 0))
            viewModel.onIntent(NotificationBadgeIntent.Refresh)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.hasUnread)
        }

    private fun inbox(unread: Long) =
        NotificationInbox(
            unreadCount = unread,
            items =
                listOf(
                    NotificationInboxItem(1, "제목", "본문", mapOf("destination" to "HOME"), isRead = false),
                    NotificationInboxItem(2, "제목", "본문", mapOf("destination" to "HOME"), isRead = true),
                ),
        )

    private class FakeInboxRepository(
        var result: Result<NotificationInbox>,
    ) : NotificationRepository {
        override suspend fun inbox(): Result<NotificationInbox> = result

        override suspend fun target(notificationId: Long): Result<NotificationTarget> = error("알림함은 목적지를 묻지 않는다")

        override suspend fun markRead(notificationId: Long): Result<Unit> = error("알림함은 읽음을 보내지 않는다")

        override suspend fun reportEntry(): Result<NotificationEntry> = error("알림함은 진입을 보고하지 않는다")
    }
}
