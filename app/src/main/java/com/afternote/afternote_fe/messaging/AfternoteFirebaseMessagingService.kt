package com.afternote.afternote_fe.messaging

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afternote.afternote_fe.R
import com.afternote.afternote_fe.notification.ServerNotificationDestination
import com.afternote.core.common.notification.NotificationDestination
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

    /**
     * 서버는 Android 로 notification 블록 없이 data-only 로 보낸다 (BE `NOTIFICATION_CONTRACT.md`).
     * 그래서 백그라운드·종료 상태에서도 이 콜백이 불리고 앱이 알림을 직접 그린다 — 그래야 탭이
     * [NotificationPendingIntentFactory] 를 거쳐 목적지로 이어진다.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val content =
            FcmNotificationContentResolver.resolve(
                notificationTitle = message.notification?.title,
                notificationBody = message.notification?.body,
                data = message.data,
                fallbackTitle = getString(R.string.fcm_notification_fallback_title),
            ) ?: return

        val occurrenceId =
            FcmNotificationIdentity.occurrenceId(
                serverNotificationId = content.serverNotificationId,
                messageId = message.messageId,
            )

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
                    NotificationPendingIntentFactory
                        .create(
                            context = this@AfternoteFirebaseMessagingService,
                            source = NOTIFICATION_SOURCE,
                            occurrenceId = occurrenceId,
                            destination = content.destination,
                            serverNotificationId = content.serverNotificationId,
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

private data class FcmNotificationContent(
    val title: String,
    val body: String?,
    val destination: NotificationDestination,
    val serverNotificationId: Long?,
)

private object FcmNotificationIdentity {
    /**
     * 서버 알림 ID 가 있으면 그것이 발생 식별자다 — FCM 이 같은 알림을 두 번 넘겨도 트레이에 한 장만
     * 남고, 탭 소비 판정도 서버 알림 하나에 한 번이 된다. 없으면 FCM message id, 그것도 없으면 새 UUID.
     */
    fun occurrenceId(
        serverNotificationId: Long?,
        messageId: String?,
    ): String =
        serverNotificationId?.let { "notification:$it" }
            ?: messageId?.takeIf(String::isNotBlank)
            ?: UUID.randomUUID().toString()

    /** NotificationManager의 `(tag, id)` identity를 프로세스 재시작 뒤에도 occurrence별로 유지한다. */
    fun notificationTag(occurrenceId: String): String = "fcm:$occurrenceId"
}

/**
 * FCM data v1 계약(BE `PushPayload`)을 읽는다. 모든 값은 문자열이다.
 *
 * 제목·본문은 notification 블록이 있으면 그쪽이 우선이다 — 서버는 이제 data 로만 보내지만, 콘솔에서 보낸
 * 시험 메시지처럼 notification 블록을 단 메시지도 그대로 보여 준다.
 *
 * 목적지는 `schemaVersion` 이 이 앱이 아는 값일 때만 따른다. 모르는 판이면 키 뜻이 바뀌었을 수 있어
 * 홈으로 연다. 알림 자체는 보여 준다 — 사용자에게 안 보이는 쪽이 더 나쁘다.
 */
private object FcmNotificationContentResolver {
    fun resolve(
        notificationTitle: String?,
        notificationBody: String?,
        data: Map<String, String>,
        fallbackTitle: String,
    ): FcmNotificationContent? {
        val title = notificationTitle.nonBlankOrNull() ?: data[KEY_TITLE].nonBlankOrNull()
        val body = notificationBody.nonBlankOrNull() ?: data[KEY_BODY].nonBlankOrNull()
        if (title == null && body == null) return null

        val schemaVersion = data[KEY_SCHEMA_VERSION].nonBlankOrNull()
        val knownSchema = schemaVersion == null || schemaVersion == SUPPORTED_SCHEMA_VERSION

        return FcmNotificationContent(
            title = title ?: fallbackTitle,
            body = body,
            destination =
                ServerNotificationDestination.toAppDestination(
                    data[KEY_DESTINATION].takeIf { knownSchema },
                ),
            serverNotificationId =
                data[KEY_NOTIFICATION_ID]
                    .nonBlankOrNull()
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L },
        )
    }

    private fun String?.nonBlankOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private const val KEY_TITLE = "title"
    private const val KEY_BODY = "body"
    private const val KEY_SCHEMA_VERSION = "schemaVersion"
    private const val KEY_DESTINATION = "destination"
    private const val KEY_NOTIFICATION_ID = "notificationId"
    private const val SUPPORTED_SCHEMA_VERSION = "1"
}
