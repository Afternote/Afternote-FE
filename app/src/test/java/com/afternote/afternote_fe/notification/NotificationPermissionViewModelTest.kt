package com.afternote.afternote_fe.notification

import com.afternote.core.domain.testing.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPermissionViewModelTest {
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
    fun `로그인 전에는 권한을 묻지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(loggedIn = false)

            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.shouldRequest)
        }

    @Test
    fun `로그인했고 아직 물은 적 없으면 요청한다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(loggedIn = true)

            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.shouldRequest)
        }

    @Test
    fun `이미 물어본 기기에서는 다시 묻지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(loggedIn = true, hasRequested = true)

            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.shouldRequest)
        }

    @Test
    fun `요청 기록을 남기면 같은 실행에서 다시 요청하지 않는다`() =
        runTest(dispatcher) {
            val store = FakeNotificationPermissionRequestStore(hasRequested = false)
            val viewModel = viewModel(loggedIn = true, store = store)

            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.shouldRequest)

            viewModel.onIntent(NotificationPermissionIntent.RecordRequest)
            advanceUntilIdle()

            assertEquals(1, store.markRequestedCalls)
            assertFalse(viewModel.uiState.value.shouldRequest)
        }

    @Test
    fun `로그아웃 뒤 재로그인해도 기록이 남아 있으면 묻지 않는다`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository(loggedIn = true)
            val viewModel =
                NotificationPermissionViewModel(
                    authRepository = authRepository,
                    store = FakeNotificationPermissionRequestStore(hasRequested = false),
                )

            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            advanceUntilIdle()
            viewModel.onIntent(NotificationPermissionIntent.RecordRequest)
            advanceUntilIdle()

            authRepository.loggedIn = false
            advanceUntilIdle()
            authRepository.loggedIn = true
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.shouldRequest)
        }

    @Test
    fun `관찰 시작 전에는 로그인과 저장소를 구독하지 않는다`() =
        runTest(dispatcher) {
            val login = TrackingFlow(true)
            val requested = TrackingFlow(false)
            val viewModel = trackedViewModel(login, requested)

            runCurrent()

            assertEquals(0, login.starts)
            assertEquals(0, requested.starts)
            assertFalse(viewModel.uiState.value.shouldRequest)
        }

    @Test
    fun `화면 중단 뒤 5초 유예가 끝나야 두 흐름을 해제한다`() =
        runTest(dispatcher) {
            val login = TrackingFlow(true)
            val requested = TrackingFlow(false)
            val viewModel = trackedViewModel(login, requested)
            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            runCurrent()

            viewModel.onIntent(NotificationPermissionIntent.ObservationStopped)
            runCurrent()
            advanceTimeBy(4_999)
            runCurrent()
            assertEquals(1, login.active)
            assertEquals(1, requested.active)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(0, login.active)
            assertEquals(0, requested.active)
        }

    @Test
    fun `중단 중 바뀐 값을 복귀 재구독에서 읽는다`() =
        runTest(dispatcher) {
            val login = TrackingFlow(false)
            val requested = TrackingFlow(false)
            val viewModel = trackedViewModel(login, requested)
            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            runCurrent()
            assertFalse(viewModel.uiState.value.shouldRequest)

            viewModel.onIntent(NotificationPermissionIntent.ObservationStopped)
            advanceUntilIdle()
            login.state.value = true
            runCurrent()
            assertFalse(viewModel.uiState.value.shouldRequest)

            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            runCurrent()
            assertTrue(viewModel.uiState.value.shouldRequest)
            assertEquals(2, login.starts)
            assertEquals(2, requested.starts)
        }

    @Test
    fun `5초 안에 복귀하면 예약한 해제를 취소하고 중복 구독하지 않는다`() =
        runTest(dispatcher) {
            val login = TrackingFlow(true)
            val requested = TrackingFlow(false)
            val viewModel = trackedViewModel(login, requested)
            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            runCurrent()
            viewModel.onIntent(NotificationPermissionIntent.ObservationStopped)
            advanceTimeBy(4_000)
            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            advanceTimeBy(2_000)
            runCurrent()

            requested.state.value = true
            runCurrent()
            assertFalse(viewModel.uiState.value.shouldRequest)
            assertEquals(1, login.starts)
            assertEquals(1, requested.starts)
            assertEquals(1, login.active)
            assertEquals(1, requested.active)
        }

    @Test
    fun `요청 기록 저장이 실패하면 기록된 것으로 위장하지 않는다`() =
        runTest(dispatcher) {
            val store = FakeNotificationPermissionRequestStore(hasRequested = false, failWrite = true)
            val viewModel = viewModel(loggedIn = true, store = store)
            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            advanceUntilIdle()

            viewModel.onIntent(NotificationPermissionIntent.RecordRequest)
            advanceUntilIdle()

            assertEquals(1, store.markRequestedCalls)
            assertTrue(viewModel.uiState.value.shouldRequest)
        }

    @Test
    fun `요청 기록의 취소는 정상 완료로 삼키지 않는다`() =
        runTest(dispatcher) {
            val cancellation = CancellationException("screen cancelled")
            var completionCause: Throwable? = null
            val store =
                object : NotificationPermissionRequestStore {
                    override val hasRequested: Flow<Boolean> = MutableStateFlow(false)

                    override suspend fun markRequested() {
                        currentCoroutineContext().job.invokeOnCompletion { completionCause = it }
                        throw cancellation
                    }
                }
            val viewModel = viewModel(loggedIn = true, store = store)
            viewModel.onIntent(NotificationPermissionIntent.ObservationStarted)
            runCurrent()

            viewModel.onIntent(NotificationPermissionIntent.RecordRequest)
            runCurrent()

            assertSame(cancellation, completionCause)
            assertTrue(viewModel.uiState.value.shouldRequest)
        }

    private fun trackedViewModel(
        login: TrackingFlow,
        requested: TrackingFlow,
    ) = NotificationPermissionViewModel(
        authRepository = FakeAuthRepository(onIsLoggedIn = { login.flow }),
        store =
            object : NotificationPermissionRequestStore {
                override val hasRequested: Flow<Boolean> = requested.flow

                override suspend fun markRequested() {
                    requested.state.value = true
                }
            },
    )

    private class TrackingFlow(
        initialValue: Boolean,
    ) {
        val state = MutableStateFlow(initialValue)
        var starts = 0
        var active = 0
        val flow: Flow<Boolean> =
            flow {
                starts++
                active++
                try {
                    emitAll(state)
                } finally {
                    active--
                }
            }
    }

    private fun viewModel(
        loggedIn: Boolean,
        hasRequested: Boolean = false,
        store: NotificationPermissionRequestStore = FakeNotificationPermissionRequestStore(hasRequested),
    ) = NotificationPermissionViewModel(
        authRepository = FakeAuthRepository(loggedIn = loggedIn),
        store = store,
    )

    private class FakeNotificationPermissionRequestStore(
        hasRequested: Boolean,
        private val failWrite: Boolean = false,
    ) : NotificationPermissionRequestStore {
        private val state = MutableStateFlow(hasRequested)

        var markRequestedCalls: Int = 0
            private set

        override val hasRequested: Flow<Boolean> = state

        override suspend fun markRequested() {
            markRequestedCalls++
            if (failWrite) throw IOException("write failed")
            state.value = true
        }
    }
}
