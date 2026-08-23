package com.afternote.afternote_fe.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FcmNotificationContentResolverTest {
    @Test
    fun `notification payload takes priority over data payload`() {
        val content =
            FcmNotificationContentResolver.resolve(
                notificationTitle = "알림 제목",
                notificationBody = "알림 본문",
                data = mapOf("title" to "data 제목", "body" to "data 본문"),
                fallbackTitle = "AFTERNOTE",
            )

        assertEquals(FcmNotificationContent(title = "알림 제목", body = "알림 본문"), content)
    }

    @Test
    fun `data body uses fallback title when title is absent`() {
        val content =
            FcmNotificationContentResolver.resolve(
                notificationTitle = null,
                notificationBody = null,
                data = mapOf("body" to "data 본문"),
                fallbackTitle = "AFTERNOTE",
            )

        assertEquals(FcmNotificationContent(title = "AFTERNOTE", body = "data 본문"), content)
    }

    @Test
    fun `blank payload does not create a notification`() {
        val content =
            FcmNotificationContentResolver.resolve(
                notificationTitle = " ",
                notificationBody = null,
                data = mapOf("body" to ""),
                fallbackTitle = "AFTERNOTE",
            )

        assertNull(content)
    }
}
