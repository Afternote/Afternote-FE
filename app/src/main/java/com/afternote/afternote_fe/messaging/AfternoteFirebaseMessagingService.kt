package com.afternote.afternote_fe.messaging

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afternote.afternote_fe.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger
import com.afternote.core.common.R as CommonR

class AfternoteFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        FcmNotificationChannel.create(this)
    }

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Log.d(TAG, "FCM installation registered")
    }

    override fun onUnregistered(installationId: String) {
        super.onUnregistered(installationId)
        Log.d(TAG, "FCM installation unregistered")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val content =
            FcmNotificationContentResolver.resolve(
                notificationTitle = message.notification?.title,
                notificationBody = message.notification?.body,
                data = message.data,
                fallbackTitle = getString(R.string.fcm_notification_fallback_title),
            ) ?: return

        showNotification(content, notificationId(message.messageId))
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        content: FcmNotificationContent,
        notificationId: Int,
    ) {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification =
            NotificationCompat
                .Builder(this, getString(R.string.fcm_notification_channel_id))
                .setSmallIcon(CommonR.drawable.core_common_logo)
                .setContentTitle(content.title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .apply {
                    content.body?.let { body ->
                        setContentText(body)
                        setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    }
                    buildContentPendingIntent(notificationId)?.let(::setContentIntent)
                }.build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun buildContentPendingIntent(notificationId: Int): PendingIntent? {
        val launchIntent =
            packageManager
                .getLaunchIntentForPackage(packageName)
                ?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                ?: return null

        return PendingIntent.getActivity(
            this,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notificationId(messageId: String?): Int = messageId?.hashCode() ?: nextNotificationId.incrementAndGet()

    private companion object {
        const val TAG = "AfternoteFCM"
        val nextNotificationId = AtomicInteger(FCM_NOTIFICATION_ID_START)
        const val FCM_NOTIFICATION_ID_START = 2_000
    }
}

internal data class FcmNotificationContent(
    val title: String,
    val body: String?,
)

internal object FcmNotificationContentResolver {
    fun resolve(
        notificationTitle: String?,
        notificationBody: String?,
        data: Map<String, String>,
        fallbackTitle: String,
    ): FcmNotificationContent? {
        val title = notificationTitle.nonBlankOrNull() ?: data[KEY_TITLE].nonBlankOrNull()
        val body = notificationBody.nonBlankOrNull() ?: data[KEY_BODY].nonBlankOrNull()
        if (title == null && body == null) return null

        return FcmNotificationContent(
            title = title ?: fallbackTitle,
            body = body,
        )
    }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private const val KEY_TITLE = "title"
    private const val KEY_BODY = "body"
}
