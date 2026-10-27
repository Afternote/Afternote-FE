package com.afternote.afternote_fe.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.afternote.core.common.notification.LegacyDailyNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * 이전 빌드가 남긴 데일리 알림 예약과 채널이 지워지는지의 회귀 기준 (#2321).
 *
 * 이전 빌드의 Worker 클래스는 지웠으므로, 같은 unique 이름으로 아무 Worker 나 넣어 업데이트 직후의
 * 기기 상태를 흉내 낸다. WorkManager 는 예약을 클래스가 아니라 이름으로 지운다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LegacyDailyNotificationTest {
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
    fun `이전 빌드의 데일리 예약과 알림 채널을 지운다`() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LegacyDailyNotification.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<NoopWorker>(24, TimeUnit.HOURS).build(),
        )
        notificationManager().createNotificationChannel(
            NotificationChannel(LegacyDailyNotification.CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT),
        )

        LegacyDailyNotification.clear(context)

        assertEquals(listOf(WorkInfo.State.CANCELLED), dailyWorkStates())
        assertNull(notificationManager().getNotificationChannel(LegacyDailyNotification.CHANNEL_ID))
    }

    @Test
    fun `지울 것이 없어도 실패하지 않는다`() {
        LegacyDailyNotification.clear(context)

        assertEquals(emptyList<WorkInfo.State>(), dailyWorkStates())
    }

    private fun dailyWorkStates(): List<WorkInfo.State> =
        WorkManager
            .getInstance(context)
            .getWorkInfosForUniqueWork(LegacyDailyNotification.UNIQUE_WORK_NAME)
            .get()
            .map(WorkInfo::state)

    private fun notificationManager(): NotificationManager = context.getSystemService(NotificationManager::class.java)

    class NoopWorker(
        context: Context,
        params: WorkerParameters,
    ) : Worker(context, params) {
        override fun doWork(): Result = Result.success()
    }
}
