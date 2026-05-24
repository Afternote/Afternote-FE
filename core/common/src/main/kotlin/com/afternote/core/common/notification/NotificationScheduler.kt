package com.afternote.core.common.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
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

        val dailyWorkRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<DailyNotificationWorker>(
                24,
                TimeUnit.HOURS,
            ).setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()

        // 앱 콜드스타트마다 호출되므로 KEEP 으로 두지 않으면 매번 next trigger 가 새 initialDelay 로 갱신돼
        // 사용자가 알림 시각 전에 앱을 켜면 알림이 영원히 미뤄질 수 있다.
        // 시간 변경 등 명시적 재예약이 필요할 때는 별도 API(예: reschedule) 로 CANCEL_AND_REENQUEUE 적용.
        workManager.enqueueUniquePeriodicWork(
            DailyNotificationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest,
        )
    }
}
