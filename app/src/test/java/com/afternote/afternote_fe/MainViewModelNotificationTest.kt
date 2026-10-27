package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.afternote_fe.notification.NotificationEntrySource
import com.afternote.afternote_fe.notification.NotificationNavigation
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.domain.repository.notification.NotificationEntry
import com.afternote.core.domain.repository.notification.NotificationInbox
import com.afternote.core.domain.repository.notification.NotificationRepository
import com.afternote.core.domain.repository.notification.NotificationTarget
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakePendingReceiverInvitationStore
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
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

            assertEquals(NotificationNavigation.Tab(NotificationDestination.TIME_LETTER), resolution.navigation)
            assertTrue(repository.targetRequests.isEmpty())
            assertTrue(repository.readRequests.isEmpty())
        }

    @Test
    fun `서버 알림은 탭한 시점의 목적지로 열고 읽음 처리한다`() =
        runTest(dispatcher) {
            val repository =
                FakeNotificationRepository(
                    target = {
                        Result.success(
                            NotificationTarget(
                                data = mapOf("destination" to "RECORD_DETAIL", "recordType" to "DAILY_QUESTION", "recordId" to "9"),
                                deleted = false,
                                toast = null,
                            ),
                        )
                    },
                )
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution =
                viewModel.resolveNotificationEntry(
                    request(
                        "notification:42",
                        serverNotificationId = 42L,
                        serverData = mapOf("destination" to "DAILY_QUESTION", "questionId" to "9"),
                    ),
                )
            advanceUntilIdle()

            assertEquals(
                NotificationNavigation.MindRecord(MindRecordRoute.RecordDetailRoute(recordId = 9L, isDiary = false)),
                resolution.navigation,
            )
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
                        Result.success(
                            NotificationTarget(data = mapOf("destination" to "TIME_LETTER_LIST"), deleted = true, toast = "삭제된 항목이에요"),
                        )
                    },
                )
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution = viewModel.resolveNotificationEntry(request("notification:7", serverNotificationId = 7L))

            assertEquals(NotificationNavigation.Tab(NotificationDestination.TIME_LETTER), resolution.navigation)
            assertTrue(resolution.deleted)
            assertEquals("삭제된 항목이에요", resolution.serverMessage)
        }

    @Test
    fun `목적지 조회가 실패하면 알림이 실어 온 data로 안쪽 화면까지 연다`() =
        runTest(dispatcher) {
            val repository = FakeNotificationRepository(target = { Result.failure(IllegalStateException("offline")) })
            val viewModel = viewModel(SavedStateHandle(), repository)

            val resolution =
                viewModel.resolveNotificationEntry(
                    request(
                        "notification:1",
                        NotificationEntrySource.FCM,
                        NotificationDestination.TIME_LETTER,
                        1L,
                        serverData = mapOf("destination" to "TIME_LETTER_DETAIL", "timeLetterId" to "5"),
                    ),
                )
            advanceUntilIdle()

            assertEquals(
                NotificationNavigation.TimeLetter(listOf(TimeLetterRoute.TimeLetterDetailRoute(5L))),
                resolution.navigation,
            )
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

            assertEquals(NotificationNavigation.Tab(NotificationDestination.MIND_RECORD), resolution.navigation)
        }

    @Test
    fun `로그인 상태로 시작하면 진입을 보고하고 보여 줄 쉬운 질문을 남긴다`() =
        runTest(dispatcher) {
            val entry = NotificationEntry(easyQuestions = listOf("오늘 가장 오래 생각한 사람은?"), procedureCancelled = false)
            val viewModel =
                viewModel(
                    SavedStateHandle(),
                    FakeNotificationRepository(entry = { Result.success(entry) }),
                    loggedIn = true,
                )
            advanceUntilIdle()

            assertEquals(entry, viewModel.entryNotice.value)

            viewModel.consumeEntryNotice()
            assertNull(viewModel.entryNotice.value)
        }

    @Test
    fun `보여 줄 것이 없는 진입 보고는 남기지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(SavedStateHandle(), FakeNotificationRepository(), loggedIn = true)
            advanceUntilIdle()

            assertNull(viewModel.entryNotice.value)
        }

    private fun viewModel(
        savedStateHandle: SavedStateHandle,
        notificationRepository: NotificationRepository = FakeNotificationRepository(),
        loggedIn: Boolean = false,
    ): MainViewModel =
        MainViewModel(
            authRepository = FakeAuthRepository(loggedIn = loggedIn),
            pendingInvitationStore = FakePendingReceiverInvitationStore(),
            notificationRepository = notificationRepository,
            savedStateHandle = savedStateHandle,
        )

    private fun request(
        occurrenceId: String,
        source: NotificationEntrySource = NotificationEntrySource.FCM,
        destination: NotificationDestination = NotificationDestination.HOME,
        serverNotificationId: Long? = null,
        serverData: Map<String, String> = emptyMap(),
    ): NotificationEntryRequest =
        NotificationEntryRequest(
            source = source,
            occurrenceId = occurrenceId,
            destination = destination,
            serverNotificationId = serverNotificationId,
            serverData = serverData,
        )

    private class FakeNotificationRepository(
        private val target: suspend () -> Result<NotificationTarget> = { error("target 은 이 테스트에서 호출되면 안 됨") },
        private val entry: suspend () -> Result<NotificationEntry> = { Result.success(NotificationEntry(emptyList(), false)) },
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

        override suspend fun inbox(): Result<NotificationInbox> = error("inbox 는 이 테스트에서 호출되면 안 됨")

        override suspend fun reportEntry(): Result<NotificationEntry> = entry()
    }
}
