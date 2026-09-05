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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import com.afternote.core.common.R as CommonR

@AndroidEntryPoint
class AfternoteFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var pushTargetSynchronizer: PushTargetSynchronizer

    override fun onCreate() {
        super.onCreate()
        FcmNotificationChannel.create(this)
    }

    /**
     * FID 가 발급·회전되면 서버에 다시 알린다 (#1493). 알리지 않으면 서버가 죽은 식별자로 발송해
     * 이 기기에는 아무것도 오지 않는다.
     *
     * 이 앱은 FID 기반 등록 모델이라 이 콜백이 레거시 `onNewToken` 자리를 대신한다.
     * Firebase SDK 의 백그라운드 스레드에서 불리므로 [runBlocking] 으로 서비스가 살아 있는 동안
     * 끝낸다 — 발사만 하고 반환하면 프로세스 종료로 요청이 잘린다.
     *
     * 이 [runBlocking] 은 `PushTargetSynchronizer` 의 뮤텍스를 기다린다. 그래도 교착이 아닌 근거는
     * **락을 쥔 코루틴이 FCM 을 기다리지 않는다**는 것이다. 뮤텍스 안에서 도는 것은 서버 `PUT`
     * 하나뿐이고, 등록 시퀀스(`FirebaseMessaging.register()`)는 락 밖에서 끝난다. 그래서 이 콜백을
     * 띄운 스레드가 `register()` Task 를 완료시키는 그 스레드라 하더라도, 기다리는 락은 FCM 과
     * 무관한 이유로 풀린다. 락 안에서 기기 식별자를 얻는 코드를 새로 넣으면 이 근거가 깨진다.
     */
    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Log.d(TAG, "FCM installation registered")
        runBlocking { pushTargetSynchronizer.onTargetIdRotated(installationId) }
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

        val occurrenceId = FcmNotificationIdentity.occurrenceId(message.messageId)

        showNotification(
            content = content,
            occurrenceId = occurrenceId,
        )
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        content: FcmNotificationContent,
        occurrenceId: String,
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
                            occurrenceId = occurrenceId,
                        )?.let(::setContentIntent)
                }.build()

        NotificationManagerCompat
            .from(this)
            .notify(FcmNotificationIdentity.notificationTag(occurrenceId), NOTIFICATION_ID, notification)
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
    fun occurrenceId(messageId: String?): String =
        messageId
            ?.takeIf(String::isNotBlank)
            ?: UUID.randomUUID().toString()

    /** NotificationManager의 `(tag, id)` identity를 프로세스 재시작 뒤에도 occurrence별로 유지한다. */
    fun notificationTag(occurrenceId: String): String = "fcm:$occurrenceId"
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
