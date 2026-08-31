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
                rawSource = "fcm",
                occurrenceId = "occurrence-1",
            )

        assertNull(request)
    }

    @Test
    fun `알 수 없거나 비어 있는 source는 거부한다`() {
        listOf(null, "", " ", "unknown").forEach { rawSource ->
            val request =
                NotificationIntentContract.resolve(
                    isNotificationEntry = true,
                    rawSource = rawSource,
                    occurrenceId = "occurrence-1",
                )

            assertNull(request)
        }
    }

    @Test
    fun `occurrence token이 없거나 비어 있으면 거부한다`() {
        listOf(null, "", " ").forEach { occurrenceId ->
            val request =
                NotificationIntentContract.resolve(
                    isNotificationEntry = true,
                    rawSource = "daily",
                    occurrenceId = occurrenceId,
                )

            assertNull(request)
        }
    }

    @Test
    fun `허용한 source와 occurrence token만 내부 요청으로 보존한다`() {
        NotificationEntrySource.entries.forEach { source ->
            val request =
                NotificationIntentContract.resolve(
                    isNotificationEntry = true,
                    rawSource = source.contractValue,
                    occurrenceId = "occurrence-${source.contractValue}",
                )

            assertEquals(source, request?.source)
            assertEquals("occurrence-${source.contractValue}", request?.occurrenceId)
        }
    }
}
