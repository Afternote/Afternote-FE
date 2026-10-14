package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakePendingReceiverInvitationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * 카카오톡 초대 토큰의 «지금 띄울지» 판정 (#944).
 *
 * `pendingInvitationToken` 은 `WhileSubscribed` 라 구독을 하나 걸어 두고 값을 읽는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelInvitationTest {
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
    fun `보관 토큰이 있고 인증 상태가 확정되면 로그인 전이라도 띄운다`() =
        runTest(dispatcher) {
            val fixture = Fixture(loggedIn = false, token = TOKEN)
            val subscription = fixture.subscribe()
            advanceUntilIdle()

            assertEquals(TOKEN, fixture.viewModel.pendingInvitationToken.value)
            subscription.cancel()
        }

    @Test
    fun `로그인 대기로 미룬 토큰은 로그인이 확정된 뒤에만 띄운다`() =
        runTest(dispatcher) {
            val fixture = Fixture(loggedIn = false, token = TOKEN)
            val subscription = fixture.subscribe()
            advanceUntilIdle()

            fixture.viewModel.deferInvitationUntilLogin(TOKEN)
            advanceUntilIdle()
            assertNull(fixture.viewModel.pendingInvitationToken.value)

            fixture.auth.loggedIn = true
            advanceUntilIdle()
            assertEquals(TOKEN, fixture.viewModel.pendingInvitationToken.value)
            subscription.cancel()
        }

    @Test
    fun `처분이 끝난 토큰은 SavedState 복원 뒤에도 다시 띄우지 않는다`() =
        runTest(dispatcher) {
            val savedStateHandle = SavedStateHandle()
            val fixture = Fixture(loggedIn = true, token = TOKEN, savedStateHandle = savedStateHandle)
            val subscription = fixture.subscribe()
            advanceUntilIdle()
            fixture.viewModel.settleInvitation(TOKEN)
            advanceUntilIdle()
            assertNull(fixture.viewModel.pendingInvitationToken.value)
            subscription.cancel()

            val restoredValues = savedStateHandle.keys().associateWith { key -> savedStateHandle.get<Any?>(key) }
            val restored = Fixture(loggedIn = true, token = TOKEN, savedStateHandle = SavedStateHandle(restoredValues))
            val restoredSubscription = restored.subscribe()
            advanceUntilIdle()
            assertNull(restored.viewModel.pendingInvitationToken.value)
            restoredSubscription.cancel()
        }

    @Test
    fun `처분 뒤 새 토큰이 들어오면 다시 띄운다`() =
        runTest(dispatcher) {
            val fixture = Fixture(loggedIn = true, token = TOKEN)
            val subscription = fixture.subscribe()
            advanceUntilIdle()
            fixture.viewModel.settleInvitation(TOKEN)
            advanceUntilIdle()

            fixture.viewModel.enqueueInvitationToken("second-token")
            advanceUntilIdle()

            assertEquals("second-token", fixture.viewModel.pendingInvitationToken.value)
            subscription.cancel()
        }

    private inner class Fixture(
        loggedIn: Boolean,
        token: String?,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) {
        val auth = FakeAuthRepository(loggedIn = loggedIn)
        val store = FakePendingReceiverInvitationStore(token = token)
        val viewModel =
            MainViewModel(
                authRepository = auth,
                pendingInvitationStore = store,
                savedStateHandle = savedStateHandle,
            )

        fun subscribe(): Job =
            viewModel.pendingInvitationToken.launchIn(
                kotlinx.coroutines.CoroutineScope(dispatcher),
            )
    }

    private companion object {
        const val TOKEN = "abcDEF123_-token"
    }
}
