package com.afternote.afternote_fe.messaging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `message id가 있으면 occurrence token으로 보존한다`() {
        assertEquals("message-1", FcmNotificationIdentity.occurrenceToken("message-1"))
    }

    @Test
    fun `message id가 없거나 blank이면 매 발생마다 고유 token을 만든다`() {
        val missing = FcmNotificationIdentity.occurrenceToken(null)
        val blank = FcmNotificationIdentity.occurrenceToken(" ")

        assertTrue(missing.isNotBlank())
        assertTrue(blank.isNotBlank())
        assertNotEquals(missing, blank)
    }

    @Test
    fun `notification tag는 occurrence마다 다르고 프로세스 상태와 무관하게 결정적이다`() {
        val first = FcmNotificationIdentity.notificationTag("occurrence-1")

        assertEquals(first, FcmNotificationIdentity.notificationTag("occurrence-1"))
        assertNotEquals(first, FcmNotificationIdentity.notificationTag("occurrence-2"))
    }
}
