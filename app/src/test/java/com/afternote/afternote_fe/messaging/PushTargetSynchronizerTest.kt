package com.afternote.afternote_fe.messaging

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.push.DevicePushTargetProvider
import com.afternote.core.domain.repository.push.PushTargetRepository
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
class PushTargetSynchronizerTest {
    // StandardTestDispatcher 로는 backgroundScope 의 collect 가 advanceUntilIdle 로도 시작되지 않아
    // 로그인 방출을 한 건도 못 본다(0829 진단). 관찰 시작을 즉시 확정시키려 Unconfined 를 쓴다.
    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `로그인이 확정되면 현재 기기 식별자를 등록한다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val authRepository = FakeAuthRepository(loggedIn = false, accessToken = "access")
            val synchronizer = synchronizer(authRepository, pushTargetRepository, deviceTargetId = "fcm-token")

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()
            assertTrue(pushTargetRepository.registered.isEmpty())

            authRepository.loggedIn = true
            advanceUntilIdle()

            assertEquals(listOf("fcm-token"), pushTargetRepository.registered)
        }

    @Test
    fun `기기 식별자를 못 얻으면 등록하지 않는다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access")
            val synchronizer = synchronizer(authRepository, pushTargetRepository, deviceTargetId = null)

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()

            assertTrue(pushTargetRepository.registered.isEmpty())
        }

    @Test
    fun `로그아웃 뒤 다시 로그인하면 한 번 더 등록한다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access")
            val synchronizer = synchronizer(authRepository, pushTargetRepository, deviceTargetId = "fcm-token")

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()

            authRepository.loggedIn = false
            advanceUntilIdle()
            authRepository.loggedIn = true
            advanceUntilIdle()

            assertEquals(listOf("fcm-token", "fcm-token"), pushTargetRepository.registered)
        }

    @Test
    fun `식별자가 회전하면 로그인 상태에서 새 값을 등록한다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val synchronizer =
                synchronizer(
                    FakeAuthRepository(loggedIn = true, accessToken = "access"),
                    pushTargetRepository,
                    deviceTargetId = "old-token",
                )

            synchronizer.onTargetIdRotated("rotated-token")

            assertEquals(listOf("rotated-token"), pushTargetRepository.registered)
        }

    @Test
    fun `로그인 전 식별자 회전은 보류한다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val synchronizer =
                synchronizer(
                    FakeAuthRepository(loggedIn = false, accessToken = null),
                    pushTargetRepository,
                    deviceTargetId = "old-token",
                )

            synchronizer.onTargetIdRotated("rotated-token")

            assertTrue(pushTargetRepository.registered.isEmpty())
        }

    @Test
    fun `로그인 등록 직후 같은 값으로 회전 통보가 와도 다시 보내지 않는다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val synchronizer =
                synchronizer(
                    FakeAuthRepository(loggedIn = true, accessToken = "access"),
                    pushTargetRepository,
                    deviceTargetId = "fid-1",
                )

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()
            // register() 성공이 그 자리에서 onRegistered 를 부르는 실제 순서를 흉내낸다.
            synchronizer.onTargetIdRotated("fid-1")
            advanceUntilIdle()

            assertEquals(listOf("fid-1"), pushTargetRepository.registered)
        }

    @Test
    fun `값이 실제로 바뀐 회전은 다시 보낸다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val synchronizer =
                synchronizer(
                    FakeAuthRepository(loggedIn = true, accessToken = "access"),
                    pushTargetRepository,
                    deviceTargetId = "fid-1",
                )

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()
            synchronizer.onTargetIdRotated("fid-2")
            advanceUntilIdle()

            assertEquals(listOf("fid-1", "fid-2"), pushTargetRepository.registered)
        }

    // 이 두 건이 리뷰 지적(#1498)의 가드다. 대역이 null 만 돌려주던 시절엔 «동기 예외» 양식이
    // 한 건도 안 덮여 있었다 — 그 예외는 collect 를 끝내 프로세스 수명 내내 등록을 못 하게 만든다.
    @Test
    fun `기기 식별자 조회가 던져도 관찰이 끊기지 않아 다음 로그인은 등록된다`() =
        runTest(dispatcher) {
            val pushTargetRepository = RecordingPushTargetRepository()
            val authRepository = FakeAuthRepository(loggedIn = false, accessToken = "access")
            var failing = true
            val synchronizer =
                synchronizer(
                    authRepository = authRepository,
                    pushTargetRepository = pushTargetRepository,
                    devicePushTargetProvider =
                        FakeDevicePushTargetProvider {
                            if (failing) throw IllegalStateException("API disabled") else "fcm-token"
                        },
                )

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()

            authRepository.loggedIn = true
            advanceUntilIdle()
            assertTrue(pushTargetRepository.registered.isEmpty())

            failing = false
            authRepository.loggedIn = false
            advanceUntilIdle()
            authRepository.loggedIn = true
            advanceUntilIdle()

            assertEquals(listOf("fcm-token"), pushTargetRepository.registered)
        }

    @Test
    fun `기기 식별자 조회가 던지면 삼키지 않고 기록한다`() =
        runTest(dispatcher) {
            val errorReporter = RecordingErrorReporter()
            val authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access")
            val synchronizer =
                synchronizer(
                    authRepository = authRepository,
                    pushTargetRepository = RecordingPushTargetRepository(),
                    devicePushTargetProvider =
                        FakeDevicePushTargetProvider { throw IllegalStateException("API disabled") },
                    errorReporter = errorReporter,
                )

            backgroundScope.launch { synchronizer.observeLogin() }
            advanceUntilIdle()

            assertEquals(listOf(IllegalStateException::class.java.name), errorReporter.recordedTypes)
            assertEquals("push_target_device_id", errorReporter.failures.single()["stage"])
        }

    @Test
    fun `회전 통보 처리가 던져도 호출부로 예외가 나가지 않는다`() =
        runTest(dispatcher) {
            val errorReporter = RecordingErrorReporter()
            val synchronizer =
                synchronizer(
                    authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access"),
                    pushTargetRepository = ThrowingPushTargetRepository(),
                    devicePushTargetProvider = FakeDevicePushTargetProvider { "fid-1" },
                    errorReporter = errorReporter,
                )

            synchronizer.onTargetIdRotated("fid-1")

            assertEquals(listOf(IllegalStateException::class.java.name), errorReporter.recordedTypes)
            assertEquals("push_target_rotated", errorReporter.failures.single()["stage"])
        }

    private fun synchronizer(
        authRepository: FakeAuthRepository,
        pushTargetRepository: PushTargetRepository,
        deviceTargetId: String?,
        errorReporter: ErrorReporter = RecordingErrorReporter(),
    ) = synchronizer(
        authRepository = authRepository,
        pushTargetRepository = pushTargetRepository,
        devicePushTargetProvider = FakeDevicePushTargetProvider { deviceTargetId },
        errorReporter = errorReporter,
    )

    private fun synchronizer(
        authRepository: FakeAuthRepository,
        pushTargetRepository: PushTargetRepository,
        devicePushTargetProvider: DevicePushTargetProvider,
        errorReporter: ErrorReporter = RecordingErrorReporter(),
    ) = PushTargetSynchronizer(
        authRepository = authRepository,
        devicePushTargetProvider = devicePushTargetProvider,
        pushTargetRepository = pushTargetRepository,
        errorReporter = errorReporter,
    )

    /** 등록·해제 두 경로가 같은 값을 본다. 던지게 만들려면 [targetId] 가 던지면 된다. */
    private class FakeDevicePushTargetProvider(
        private val targetId: () -> String?,
    ) : DevicePushTargetProvider {
        override suspend fun currentTargetId(): String? = targetId()

        override suspend fun existingTargetId(): String? = targetId()
    }

    // recordFailure 는 문구 유출을 막으려 예외를 redact 한 사본으로 바꾸고 원래 타입은
    // "error_type" 속성으로 옮긴다. 그래서 단언은 예외 인스턴스가 아니라 이 속성으로 한다.
    private class RecordingErrorReporter : ErrorReporter {
        val failures = mutableListOf<Map<String, String>>()

        val recordedTypes: List<String?>
            get() = failures.map { it["error_type"] }

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            failures += attributes
        }
    }

    private class ThrowingPushTargetRepository : PushTargetRepository {
        override suspend fun register(targetId: String): Result<Unit> = throw IllegalStateException("boom")

        override suspend fun unregister(targetId: String): Result<Unit> = throw IllegalStateException("boom")
    }

    private class RecordingPushTargetRepository : PushTargetRepository {
        val registered = mutableListOf<String>()
        val unregistered = mutableListOf<String>()

        override suspend fun register(targetId: String): Result<Unit> {
            registered += targetId
            return Result.success(Unit)
        }

        override suspend fun unregister(targetId: String): Result<Unit> {
            unregistered += targetId
            return Result.success(Unit)
        }
    }
}
