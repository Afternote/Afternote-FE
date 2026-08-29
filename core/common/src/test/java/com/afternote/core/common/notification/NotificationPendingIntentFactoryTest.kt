package com.afternote.core.common.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NotificationPendingIntentFactoryTest {
    @Test
    fun `같은 source와 occurrence token은 같은 action을 만든다`() {
        val first = NotificationPendingIntentFactory.notificationAction("fcm", "message-1")
        val second = NotificationPendingIntentFactory.notificationAction("fcm", "message-1")

        assertEquals(first, second)
    }

    @Test
    fun `source 또는 occurrence token이 다르면 action이 다르다`() {
        val baseline = NotificationPendingIntentFactory.notificationAction("fcm", "message-1")

        assertNotEquals(baseline, NotificationPendingIntentFactory.notificationAction("daily", "message-1"))
        assertNotEquals(baseline, NotificationPendingIntentFactory.notificationAction("fcm", "message-2"))
    }

    @Test
    fun `Java hash가 충돌하는 occurrence token도 다른 action을 만든다`() {
        assertEquals("Aa".hashCode(), "BB".hashCode())

        val first = NotificationPendingIntentFactory.notificationAction("fcm", "Aa")
        val second = NotificationPendingIntentFactory.notificationAction("fcm", "BB")

        assertNotEquals(first, second)
    }

    @Test
    fun `source와 token의 경계가 달라지면 이어 붙인 문자가 같아도 action이 다르다`() {
        val first = NotificationPendingIntentFactory.notificationAction("a", "bc")
        val second = NotificationPendingIntentFactory.notificationAction("ab", "c")

        assertNotEquals(first, second)
    }

    @Test
    fun `빈 identity 구성요소는 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationPendingIntentFactory.notificationAction(" ", "token")
        }
        assertThrows(IllegalArgumentException::class.java) {
            NotificationPendingIntentFactory.notificationAction("fcm", "")
        }
    }
}
