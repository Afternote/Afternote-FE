package com.afternote.core.common.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager

/**
 * 앱이 직접 예약하던 매일 09:00 데일리 알림의 흔적을 지운다 (#2321).
 *
 * 기록 재촉 알림은 이제 서버가 보낸다(BE 알림 B). 서버는 사용자가 고른 시각·요일에 맞추고, 하루 일반
 * 알림 1건 상한과 22~08시 이월을 다른 알림과 함께 판정한다. 앱이 따로 울리면 같은 재촉이 두 번 오고
 * 그 정책을 앱이 깬다. 그래서 예약을 그만두고, 이전 빌드가 남긴 WorkManager 예약과 알림 채널을 지운다.
 *
 * 둘 다 없는 것을 지워도 아무 일이 없어 실행마다 불러도 된다. 이름은 이전 빌드가 쓰던 값 그대로다.
 */
object LegacyDailyNotification {
    fun clear(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        NotificationManagerCompat.from(context).deleteNotificationChannel(CHANNEL_ID)
    }

    const val UNIQUE_WORK_NAME = "daily_notification_work"
    const val CHANNEL_ID = "DAILY_CHANNEL_ID"
}
