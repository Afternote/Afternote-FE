package com.afternote.core.startup

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import com.afternote.core.common.notification.NotificationScheduler

/**
 * 앱 기동 시 일일 알림 WorkManager 예약.
 * `app`이 `core:common`의 [NotificationScheduler]를 직접 호출해도 되지만, App Startup으로 초기화를 한곳에 모은다.
 *
 * [WorkManagerInitializer]를 의존성에 넣어 [androidx.work.WorkManager]가 먼저 준비된 뒤 스케줄링하도록 한다.
 */
class DailyNotificationInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // TODO: 설정(DataStore 등)에서 시·분 읽기.
        NotificationScheduler.scheduleDailyNotification(
            context.applicationContext,
            hour = 9,
            minute = 0,
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(WorkManagerInitializer::class.java)
}
