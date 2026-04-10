package com.afternote.core.common.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 일 단위 알림을 WorkManager로 예약한다.
 *
 * **주의:** [PeriodicWorkRequest]는 배터리·Doze 등으로 지정 시각에 정확히 맞추지 못하고 시간이 밀릴 수 있다(Drift).
 * 사용자 설정 시각에 정확히 울려야 하면 `AlarmManager.setExactAndAllowWhileIdle` 등으로 전환해야 한다.
 */
object NotificationScheduler {
    fun scheduleDailyNotification(
        context: Context,
        hour: Int,
        minute: Int,
    ) {
        val workManager = WorkManager.getInstance(context)

        val now = LocalDateTime.now()
        var targetTime =
            now
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0)

        if (targetTime.isBefore(now)) {
            targetTime = targetTime.plusDays(1)
        }

        val initialDelay = Duration.between(now, targetTime).toMillis()

        val dailyWorkRequest =
            PeriodicWorkRequestBuilder<DailyNotificationWorker>(
                24,
                TimeUnit.HOURS,
            ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()

        // 시간 변경 등으로 기존 예약을 덮어써야 하므로 UPDATE 정책 사용 (KEEP 아님).
        workManager.enqueueUniquePeriodicWork(
            DailyNotificationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest,
        )
    }
}
