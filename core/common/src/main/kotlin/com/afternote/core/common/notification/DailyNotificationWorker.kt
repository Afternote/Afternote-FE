package com.afternote.core.common.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.afternote.core.common.R
import java.util.UUID

class DailyNotificationWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        createNotificationChannel()
        showNotification()
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showNotification() {
        val pendingIntent =
            NotificationPendingIntentFactory.create(
                context = applicationContext,
                source = NOTIFICATION_SOURCE,
                occurrenceId = UUID.randomUUID().toString(),
            )

        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.core_common_logo)
                .setContentTitle(applicationContext.getString(R.string.core_common_notification_daily_title))
                .setContentText(applicationContext.getString(R.string.core_common_notification_daily_text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .apply {
                    pendingIntent?.let { setContentIntent(it) }
                }.build()

        // 런타임에서 POST_NOTIFICATIONS를 확인하므로 MissingPermission Lint는 억제해도 안전함.
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val name = applicationContext.getString(R.string.core_common_notification_daily_channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    companion object {
        /** WorkManager unique work 이름 및 태그 ([NotificationScheduler]와 동일 값 유지). */
        const val UNIQUE_WORK_NAME = "daily_notification_work"

        private const val CHANNEL_ID = "DAILY_CHANNEL_ID"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_SOURCE = "daily"
    }
}
