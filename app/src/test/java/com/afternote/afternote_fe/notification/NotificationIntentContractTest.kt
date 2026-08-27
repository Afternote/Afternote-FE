package com.afternote.afternote_fe.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationIntentContractTest {
    @Test
    fun `marker 없는 일반 launcher 진입은 알림 이벤트가 아니다`() {
        val request =
            NotificationIntentContract.resolve(
                isNotificationEntry = false,
                occurrenceToken = "occurrence-1",
                rawTarget = "time_letter",
                rawParameters = emptyMap(),
            )

        assertNull(request)
    }

    @Test
    fun `marker가 있어도 occurrence token이 없으면 이벤트를 만들지 않는다`() {
        val request =
            NotificationIntentContract.resolve(
                isNotificationEntry = true,
                occurrenceToken = " ",
                rawTarget = "time_letter",
                rawParameters = emptyMap(),
            )

        assertNull(request)
    }

    @Test
    fun `목적지가 없거나 unknown 또는 잘못된 타입이면 Home으로 제한한다`() {
        listOf(null, "unknown", 7L).forEach { rawTarget ->
            val request =
                NotificationIntentContract.resolve(
                    isNotificationEntry = true,
                    occurrenceToken = "occurrence-1",
                    rawTarget = rawTarget,
                    rawParameters = mapOf("id" to 3L),
                )

            assertEquals(NotificationTopLevelDestination.HOME, request?.destination)
            assertEquals(emptyMap<String, NotificationPrimitiveParameter>(), request?.parameters)
        }
    }

    @Test
    fun `whitelist 최상위 목적지와 primitive parameter만 보존한다`() {
        NotificationTopLevelDestination.entries.forEach { destination ->
            val request =
                NotificationIntentContract.resolve(
                    isNotificationEntry = true,
                    occurrenceToken = "occurrence-${destination.contractValue}",
                    rawTarget = destination.contractValue,
                    rawParameters =
                        mapOf(
                            "text" to "value",
                            "enabled" to true,
                            "page" to 2,
                            "itemId" to 9L,
                        ),
                )

            assertEquals(destination, request?.destination)
            assertEquals(NotificationPrimitiveParameter.Text("value"), request?.parameters?.get("text"))
            assertEquals(NotificationPrimitiveParameter.Flag(true), request?.parameters?.get("enabled"))
            assertEquals(NotificationPrimitiveParameter.Integer(2), request?.parameters?.get("page"))
            assertEquals(NotificationPrimitiveParameter.LongInteger(9L), request?.parameters?.get("itemId"))
        }
    }

    @Test
    fun `지원하지 않는 payload 값은 목적지와 함께 Home fallback으로 제한한다`() {
        val request =
            NotificationIntentContract.resolve(
                isNotificationEntry = true,
                occurrenceToken = "occurrence-1",
                rawTarget = "afternote",
                rawParameters = mapOf("nested" to listOf(1L)),
            )

        assertEquals(NotificationTopLevelDestination.HOME, request?.destination)
        assertEquals(emptyMap<String, NotificationPrimitiveParameter>(), request?.parameters)
    }

    @Test
    fun `payload 역직렬화가 실패해도 marker와 token이 유효하면 Home으로 제한한다`() {
        val request =
            NotificationIntentContract.resolvePayloadSafely(
                isNotificationEntry = true,
                occurrenceToken = "occurrence-1",
            ) {
                throw IllegalStateException("bad parcel")
            }

        assertEquals(NotificationTopLevelDestination.HOME, request?.destination)
        assertEquals("occurrence-1", request?.occurrenceToken)
    }
}
