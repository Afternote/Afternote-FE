package com.afternote.afternote_fe.notification

import com.afternote.core.common.notification.NotificationDestination
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
                rawDestination = "home",
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
                    rawDestination = "home",
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
                    rawDestination = "home",
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
                    rawDestination = "home",
                )

            assertEquals(source, request?.source)
            assertEquals("occurrence-${source.contractValue}", request?.occurrenceId)
        }
    }

    @Test
    fun `계약에 있는 목적지는 그대로 보존한다`() {
        NotificationDestination.entries.forEach { destination ->
            val request =
                NotificationIntentContract.resolve(
                    isNotificationEntry = true,
                    rawSource = "daily",
                    occurrenceId = "occurrence-1",
                    rawDestination = destination.contractValue,
                )

            assertEquals(destination, request?.destination)
        }
    }

    @Test
    fun `목적지가 없거나 해석되지 않아도 알림 자체는 살리고 홈으로 폴백한다`() {
        listOf(null, "", "   ", "unknown", "HOME", "afternote_home").forEach { rawDestination ->
            val request =
                NotificationIntentContract.resolve(
                    isNotificationEntry = true,
                    rawSource = "fcm",
                    occurrenceId = "occurrence-1",
                    rawDestination = rawDestination,
                )

            assertEquals(NotificationDestination.HOME, request?.destination)
        }
    }

    @Test
    fun `목적지는 소비 identity에 들어가지 않는다`() {
        val toHome =
            NotificationIntentContract.resolve(
                isNotificationEntry = true,
                rawSource = "fcm",
                occurrenceId = "occurrence-1",
                rawDestination = NotificationDestination.HOME.contractValue,
            )
        val toTimeLetter =
            NotificationIntentContract.resolve(
                isNotificationEntry = true,
                rawSource = "fcm",
                occurrenceId = "occurrence-1",
                rawDestination = NotificationDestination.TIME_LETTER.contractValue,
            )

        assertEquals(toHome?.identityKey, toTimeLetter?.identityKey)
    }
}
