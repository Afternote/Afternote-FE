package com.afternote.core.common.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.afternote.core.common.R

class DailyNotificationWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        createNotificationChannel()
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val pendingIntent = buildContentPendingIntent()

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

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * 앱 런처와 동일한 진입점으로 연결한다.
     * [core:common]은 app 모듈에 의존할 수 없으므로 [Intent]에 MainActivity를 직접 넣지 않고
     * 패키지의 기본 실행 Intent를 사용한다.
     */
    private fun buildContentPendingIntent(): PendingIntent? {
        val launchIntent =
            applicationContext.packageManager
                .getLaunchIntentForPackage(applicationContext.packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                ?: return null

        return PendingIntent.getActivity(
            applicationContext,
            CONTENT_INTENT_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.core_common_notification_daily_channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    companion object {
        /** WorkManager unique work 이름 및 태그 ([NotificationScheduler]와 동일 값 유지). */
        const val UNIQUE_WORK_NAME = "daily_notification_work"

        private const val CHANNEL_ID = "DAILY_CHANNEL_ID"
        private const val NOTIFICATION_ID = 1001
        private const val CONTENT_INTENT_REQUEST_CODE = 0
    }
}
