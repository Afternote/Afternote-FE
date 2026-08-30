package com.afternote.afternote_fe.messaging

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afternote.afternote_fe.R
import com.afternote.core.common.notification.NotificationPendingIntentFactory
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID
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

        val occurrenceToken = FcmNotificationIdentity.occurrenceToken(message.messageId)

        showNotification(
            content = content,
            occurrenceToken = occurrenceToken,
        )
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        content: FcmNotificationContent,
        occurrenceToken: String,
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
                    // BE 목적지 계약 전에는 FCM data를 화면 이동 payload로 연결하지 않는다.
                    NotificationPendingIntentFactory
                        .create(
                            context = this@AfternoteFirebaseMessagingService,
                            source = NOTIFICATION_SOURCE,
                            occurrenceToken = occurrenceToken,
                        )?.let(::setContentIntent)
                }.build()

        NotificationManagerCompat
            .from(this)
            .notify(FcmNotificationIdentity.notificationTag(occurrenceToken), NOTIFICATION_ID, notification)
    }

    private companion object {
        const val TAG = "AfternoteFCM"
        const val NOTIFICATION_SOURCE = "fcm"
        const val NOTIFICATION_ID = 2_000
    }
}

internal data class FcmNotificationContent(
    val title: String,
    val body: String?,
)

internal object FcmNotificationIdentity {
    fun occurrenceToken(messageId: String?): String =
        messageId
            ?.takeIf(String::isNotBlank)
            ?: UUID.randomUUID().toString()

    /** NotificationManager의 `(tag, id)` identity를 프로세스 재시작 뒤에도 occurrence별로 유지한다. */
    fun notificationTag(occurrenceToken: String): String = "fcm:$occurrenceToken"
}

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
