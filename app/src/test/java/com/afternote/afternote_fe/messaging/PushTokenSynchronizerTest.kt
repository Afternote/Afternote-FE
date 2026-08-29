package com.afternote.afternote_fe.messaging

import com.afternote.core.domain.push.DevicePushTokenProvider
import com.afternote.core.domain.repository.push.PushTokenRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushTokenSynchronizerTest {
    // StandardTestDispatcher 로는 backgroundScope 의 collect 가 advanceUntilIdle 로도 시작되지 않아
    // 로그인 방출을 한 건도 못 본다(0829 진단). 관찰 시작을 즉시 확정시키려 Unconfined 를 쓴다.
    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `로그인이 확정되면 현재 기기 토큰을 등록한다`() =
        runTest(dispatcher) {
            val pushTokenRepository = RecordingPushTokenRepository()
            val authRepository = FakeAuthRepository(loggedIn = false, accessToken = "access")
            val synchronizer = synchronizer(authRepository, pushTokenRepository, deviceToken = "fcm-token")

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()
            assertTrue(pushTokenRepository.registered.isEmpty())

            authRepository.loggedIn = true
            advanceUntilIdle()

            assertEquals(listOf("fcm-token"), pushTokenRepository.registered)
        }

    @Test
    fun `기기 토큰을 못 얻으면 등록하지 않는다`() =
        runTest(dispatcher) {
            val pushTokenRepository = RecordingPushTokenRepository()
            val authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access")
            val synchronizer = synchronizer(authRepository, pushTokenRepository, deviceToken = null)

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()

            assertTrue(pushTokenRepository.registered.isEmpty())
        }

    @Test
    fun `로그아웃 뒤 다시 로그인하면 한 번 더 등록한다`() =
        runTest(dispatcher) {
            val pushTokenRepository = RecordingPushTokenRepository()
            val authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access")
            val synchronizer = synchronizer(authRepository, pushTokenRepository, deviceToken = "fcm-token")

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()

            authRepository.loggedIn = false
            advanceUntilIdle()
            authRepository.loggedIn = true
            advanceUntilIdle()

            assertEquals(listOf("fcm-token", "fcm-token"), pushTokenRepository.registered)
        }

    @Test
    fun `토큰이 회전하면 로그인 상태에서 새 토큰을 등록한다`() =
        runTest(dispatcher) {
            val pushTokenRepository = RecordingPushTokenRepository()
            val synchronizer =
                synchronizer(
                    FakeAuthRepository(loggedIn = true, accessToken = "access"),
                    pushTokenRepository,
                    deviceToken = "old-token",
                )

            synchronizer.onTokenRotated("rotated-token")

            assertEquals(listOf("rotated-token"), pushTokenRepository.registered)
        }

    @Test
    fun `로그인 전 토큰 회전은 보류한다`() =
        runTest(dispatcher) {
            val pushTokenRepository = RecordingPushTokenRepository()
            val synchronizer =
                synchronizer(
                    FakeAuthRepository(loggedIn = false, accessToken = null),
                    pushTokenRepository,
                    deviceToken = "old-token",
                )

            synchronizer.onTokenRotated("rotated-token")

            assertTrue(pushTokenRepository.registered.isEmpty())
        }

    private fun synchronizer(
        authRepository: FakeAuthRepository,
        pushTokenRepository: PushTokenRepository,
        deviceToken: String?,
    ) = PushTokenSynchronizer(
        authRepository = authRepository,
        devicePushTokenProvider = DevicePushTokenProvider { deviceToken },
        pushTokenRepository = pushTokenRepository,
    )

    private class RecordingPushTokenRepository : PushTokenRepository {
        val registered = mutableListOf<String>()
        val unregistered = mutableListOf<String>()

        override suspend fun register(token: String): Result<Unit> {
            registered += token
            return Result.success(Unit)
        }

        override suspend fun unregister(token: String): Result<Unit> {
            unregistered += token
            return Result.success(Unit)
        }
    }
}
