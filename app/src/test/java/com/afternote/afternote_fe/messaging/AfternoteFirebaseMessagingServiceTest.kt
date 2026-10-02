package com.afternote.afternote_fe.messaging

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.os.Bundle
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * FCM 메시지 한 건이 실제로 어떤 알림이 되는지의 회귀 기준.
 *
 * 제목·본문 결정과 occurrence 식별은 `AfternoteFirebaseMessagingService.kt` 안에서만 쓰이는
 * file-local 구현이라 직접 부르지 않는다 (#1671). 대신 Firebase 가 부르는 `onMessageReceived`
 * 에 진짜 [RemoteMessage] 를 넣고 **게시된 알림**을 읽는다.
 *
 * 그래서 «occurrence 별 tag» 같은 내부 개념도 밖에서 보이는 결과로 바꿔 단언한다 — 같은 발생을
 * 두 번 받으면 알림이 하나로 합쳐지고, 다른 발생이면 둘로 남는 것이 그 tag 가 존재하는 이유다.
 *
 * `onCreate()` 는 부르지 않는다. Hilt 진입점이라 주입이 먼저 도는데 이 테스트가 검증하는 경로는
 * 주입된 협력자를 쓰지 않는다. 알림 채널은 `GlobalApplication` 이 하듯 여기서 직접 만든다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AfternoteFirebaseMessagingServiceTest {
    private lateinit var service: AfternoteFirebaseMessagingService

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        FcmNotificationChannel.create(application)
        service = Robolectric.buildService(AfternoteFirebaseMessagingService::class.java).get()
    }

    @Test
    fun `notification payload가 data payload보다 우선한다`() {
        service.onMessageReceived(
            remoteMessage(
                notificationTitle = "알림 제목",
                notificationBody = "알림 본문",
                data = mapOf("title" to "data 제목", "body" to "data 본문"),
            ),
        )

        assertEquals("알림 제목", contentTitleOfSingleNotification())
        assertEquals("알림 본문", contentTextOfSingleNotification())
    }

    @Test
    fun `제목이 없으면 fallback 제목으로 알림을 띄운다`() {
        service.onMessageReceived(remoteMessage(data = mapOf("body" to "data 본문")))

        assertEquals("AFTERNOTE", contentTitleOfSingleNotification())
        assertEquals("data 본문", contentTextOfSingleNotification())
    }

    @Test
    fun `제목도 본문도 없는 메시지는 알림을 만들지 않는다`() {
        service.onMessageReceived(
            remoteMessage(notificationTitle = " ", data = mapOf("body" to "")),
        )

        assertEquals(0, postedNotifications().size)
    }

    @Test
    fun `같은 message id를 다시 받으면 알림이 하나로 합쳐진다`() {
        service.onMessageReceived(remoteMessage(messageId = "message-1", notificationTitle = "첫 번째"))
        service.onMessageReceived(remoteMessage(messageId = "message-1", notificationTitle = "다시 온 같은 발생"))

        assertEquals(1, postedNotifications().size)
        assertEquals("다시 온 같은 발생", contentTitleOfSingleNotification())
    }

    @Test
    fun `서로 다른 message id는 각각 남는다`() {
        service.onMessageReceived(remoteMessage(messageId = "message-1", notificationTitle = "첫 번째"))
        service.onMessageReceived(remoteMessage(messageId = "message-2", notificationTitle = "두 번째"))

        assertEquals(2, postedNotifications().size)
    }

    @Test
    fun `Java hash가 충돌하는 message id도 서로 다른 알림으로 남는다`() {
        assertEquals("Aa".hashCode(), "BB".hashCode())

        service.onMessageReceived(remoteMessage(messageId = "Aa", notificationTitle = "첫 번째"))
        service.onMessageReceived(remoteMessage(messageId = "BB", notificationTitle = "두 번째"))

        assertEquals(2, postedNotifications().size)
    }

    @Test
    fun `message id가 없는 메시지는 발생마다 따로 남는다`() {
        service.onMessageReceived(remoteMessage(messageId = null, notificationTitle = "첫 번째"))
        service.onMessageReceived(remoteMessage(messageId = " ", notificationTitle = "두 번째"))

        assertEquals(2, postedNotifications().size)
    }

    private fun postedNotifications(): List<Notification> {
        val manager = RuntimeEnvironment.getApplication().getSystemService(NotificationManager::class.java)
        return shadowOf(manager).allNotifications
    }

    private fun contentTitleOfSingleNotification(): String = shadowOf(singleNotification()).contentTitle.toString()

    private fun contentTextOfSingleNotification(): String = shadowOf(singleNotification()).contentText.toString()

    private fun singleNotification(): Notification {
        val notifications = postedNotifications()
        assertEquals("알림이 정확히 하나 게시돼야 한다", 1, notifications.size)
        return notifications.single()
    }

    /**
     * FCM 이 전달하는 그대로의 wire payload 를 만든다. `gcm.n.*` 는 notification payload,
     * 접두사 없는 키는 data payload, `google.message_id` 는 발생 식별자다.
     */
    private fun remoteMessage(
        notificationTitle: String? = null,
        notificationBody: String? = null,
        data: Map<String, String> = emptyMap(),
        messageId: String? = "message-1",
    ): RemoteMessage =
        RemoteMessage(
            Bundle().apply {
                messageId?.let { putString("google.message_id", it) }
                if (notificationTitle != null || notificationBody != null) {
                    putString("gcm.n.e", "1")
                    notificationTitle?.let { putString("gcm.n.title", it) }
                    notificationBody?.let { putString("gcm.n.body", it) }
                }
                data.forEach { (key, value) -> putString(key, value) }
            },
        )
}
