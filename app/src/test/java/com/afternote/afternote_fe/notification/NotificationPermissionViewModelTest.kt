package com.afternote.afternote_fe.notification

import com.afternote.core.domain.testing.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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

            backgroundScope.subscribe(viewModel)
            advanceUntilIdle()

            assertFalse(viewModel.shouldRequest.value)
        }

    @Test
    fun `로그인했고 아직 물은 적 없으면 요청한다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(loggedIn = true)

            backgroundScope.subscribe(viewModel)
            advanceUntilIdle()

            assertTrue(viewModel.shouldRequest.value)
        }

    @Test
    fun `이미 물어본 기기에서는 다시 묻지 않는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(loggedIn = true, hasRequested = true)

            backgroundScope.subscribe(viewModel)
            advanceUntilIdle()

            assertFalse(viewModel.shouldRequest.value)
        }

    @Test
    fun `요청 기록을 남기면 같은 실행에서 다시 요청하지 않는다`() =
        runTest(dispatcher) {
            val store = FakeNotificationPermissionRequestStore(hasRequested = false)
            val viewModel = viewModel(loggedIn = true, store = store)

            backgroundScope.subscribe(viewModel)
            advanceUntilIdle()
            assertTrue(viewModel.shouldRequest.value)

            viewModel.markRequested()
            advanceUntilIdle()

            assertEquals(1, store.markRequestedCalls)
            assertFalse(viewModel.shouldRequest.value)
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

            backgroundScope.subscribe(viewModel)
            advanceUntilIdle()
            viewModel.markRequested()
            advanceUntilIdle()

            authRepository.loggedIn = false
            advanceUntilIdle()
            authRepository.loggedIn = true
            advanceUntilIdle()

            assertFalse(viewModel.shouldRequest.value)
        }

    /** `shouldRequest` 는 `WhileSubscribed` 라 구독자가 없으면 upstream 이 돌지 않는다. */
    private fun CoroutineScope.subscribe(viewModel: NotificationPermissionViewModel) {
        launch { viewModel.shouldRequest.collect { } }
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
    ) : NotificationPermissionRequestStore {
        private val state = MutableStateFlow(hasRequested)

        var markRequestedCalls: Int = 0
            private set

        override val hasRequested: Flow<Boolean> = state

        override suspend fun markRequested() {
            markRequestedCalls++
            state.value = true
        }
    }
}
