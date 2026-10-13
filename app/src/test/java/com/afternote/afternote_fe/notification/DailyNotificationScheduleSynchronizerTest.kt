package com.afternote.afternote_fe.notification

import android.app.Application
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.afternote.core.common.notification.DailyNotificationWorker
import com.afternote.core.common.notification.NotificationScheduler
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 데일리 알림 예약이 로그인 상태를 따라가는지의 회귀 기준 (#2146).
 *
 * 대역을 두지 않고 실제 WorkManager 에 넣고 지운 예약을 읽는다. 보는 것은 unique work 의 상태다.
 * 로그인 상태에서만 ENQUEUED 이고, 로그아웃되면 CANCELLED 가 된다.
 *
 * `application` 을 `Application` 으로 둔 것은 `GlobalApplication` 이 Hilt 진입점이라 주입이 먼저
 * 돌기 때문이다. 이 테스트는 관찰자를 직접 만든다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DailyNotificationScheduleSynchronizerTest {
    // PushTargetSynchronizerTest 와 같은 이유다. StandardTestDispatcher 로는 backgroundScope 의 collect 가 시작되지 않는다.
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .setTaskExecutor(SynchronousExecutor())
                .build(),
        )
    }

    @Test
    fun `로그아웃 상태로 시작하면 데일리 알림을 예약하지 않는다`() =
        runTest(dispatcher) {
            backgroundScope.launch { synchronizer(FakeAuthRepository(loggedIn = false)).observeLogin() }
            advanceUntilIdle()

            assertTrue(dailyWork().isEmpty())
        }

    @Test
    fun `로그아웃 상태로 켜지면 세션 없이 넣어 둔 예약을 지운다`() =
        runTest(dispatcher) {
            // 이전 빌드의 Initializer 가 세션을 보지 않고 넣어 둔 예약.
            NotificationScheduler.scheduleDailyNotification(context, hour = 9, minute = 0)
            assertEquals(WorkInfo.State.ENQUEUED, dailyState())

            backgroundScope.launch { synchronizer(FakeAuthRepository(loggedIn = false)).observeLogin() }
            advanceUntilIdle()

            assertEquals(WorkInfo.State.CANCELLED, dailyState())
        }

    @Test
    fun `로그인하면 예약하고 로그아웃하면 지운다`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository(loggedIn = false)
            backgroundScope.launch { synchronizer(authRepository).observeLogin() }
            advanceUntilIdle()

            authRepository.loggedIn = true
            advanceUntilIdle()
            assertEquals(WorkInfo.State.ENQUEUED, dailyState())

            authRepository.loggedIn = false
            advanceUntilIdle()
            assertEquals(WorkInfo.State.CANCELLED, dailyState())
        }

    @Test
    fun `로그아웃 뒤 다시 로그인하면 새로 예약한다`() =
        runTest(dispatcher) {
            val authRepository = FakeAuthRepository(loggedIn = true)
            backgroundScope.launch { synchronizer(authRepository).observeLogin() }
            advanceUntilIdle()

            authRepository.loggedIn = false
            advanceUntilIdle()
            authRepository.loggedIn = true
            advanceUntilIdle()

            assertEquals(WorkInfo.State.ENQUEUED, dailyState())
        }

    @Test
    fun `로그인한 채 켜지면 이미 있는 예약을 갈아 끼우지 않는다`() =
        runTest(dispatcher) {
            // 갈아 끼우면 다음 발화 시각이 새 initialDelay 로 밀린다. 알림 시각 전에 앱을 켜는 사용자는 알림을 영영 못 받는다.
            NotificationScheduler.scheduleDailyNotification(context, hour = 9, minute = 0)
            val existing = dailyWork().single().id

            backgroundScope.launch { synchronizer(FakeAuthRepository(loggedIn = true)).observeLogin() }
            advanceUntilIdle()

            val current = dailyWork().single()
            assertEquals(existing, current.id)
            assertEquals(WorkInfo.State.ENQUEUED, current.state)
        }

    private fun synchronizer(authRepository: FakeAuthRepository) =
        DailyNotificationScheduleSynchronizer(
            context = context,
            authRepository = authRepository,
            errorReporter = FailingErrorReporter(),
        )

    private fun dailyWork(): List<WorkInfo> =
        WorkManager
            .getInstance(context)
            .getWorkInfosForUniqueWork(DailyNotificationWorker.UNIQUE_WORK_NAME)
            .get()

    private fun dailyState(): WorkInfo.State = dailyWork().single().state

    /** 이 테스트의 경로에서는 실패가 없어야 한다. 기록되면 그 자리에서 드러낸다. */
    private class FailingErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ): Unit = throw AssertionError("예상하지 못한 실패 기록: $attributes", throwable)
    }
}
