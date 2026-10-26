package com.afternote.afternote_fe.notification

import android.app.Application
import android.content.Intent
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.common.notification.NotificationPendingIntentFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * 알림 진입 Intent 해석 계약의 회귀 기준.
 *
 * 판정 자체는 `fromIntent()` 뒤의 file-local 구현이라 직접 부르지 않는다 (#1671). 대신
 * `MainActivity` 가 실제로 부르는 `fromIntent(Intent)` 에 진짜 [Intent] 를 넣는다 — 그래야
 * extra 키 이름과 타입까지 계약에 포함된다. 값을 꺼내는 쪽만 검증하면 키를 바꿔 쓰는 회귀는
 * 지나간다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NotificationIntentContractTest {
    @Test
    fun `marker 없는 일반 launcher 진입은 알림 이벤트가 아니다`() {
        assertNull(NotificationIntentContract.fromIntent(Intent(Intent.ACTION_MAIN)))
        assertNull(NotificationIntentContract.fromIntent(notificationIntent(isNotificationEntry = false)))
    }

    @Test
    fun `알 수 없거나 비어 있는 source는 거부한다`() {
        listOf(null, "", " ", "unknown").forEach { rawSource ->
            val request = NotificationIntentContract.fromIntent(notificationIntent(source = rawSource))

            assertNull("source '$rawSource' 를 받으면 안 된다", request)
        }
    }

    @Test
    fun `occurrence token이 없거나 비어 있으면 거부한다`() {
        listOf(null, "", " ").forEach { occurrenceId ->
            val request = NotificationIntentContract.fromIntent(notificationIntent(occurrenceId = occurrenceId))

            assertNull("occurrence '$occurrenceId' 를 받으면 안 된다", request)
        }
    }

    @Test
    fun `허용한 source와 occurrence token만 내부 요청으로 보존한다`() {
        NotificationEntrySource.entries.forEach { source ->
            val request =
                NotificationIntentContract.fromIntent(
                    notificationIntent(
                        source = source.contractValue,
                        occurrenceId = "occurrence-${source.contractValue}",
                    ),
                )

            assertEquals(source, request?.source)
            assertEquals("occurrence-${source.contractValue}", request?.occurrenceId)
        }
    }

    @Test
    fun `알림을 만든 PendingIntent 를 그대로 되읽는다`() {
        // 쓰는 쪽(core:common)과 읽는 쪽(app)이 같은 키 계약을 쓰는지는 왕복으로만 확인된다.
        val pendingIntent =
            NotificationPendingIntentFactory.create(
                context = RuntimeEnvironment.getApplication(),
                source = NotificationEntrySource.DAILY.contractValue,
                occurrenceId = "occurrence-1",
                destination = NotificationDestination.HOME,
            )
        assertNotNull("런처 Intent 를 찾지 못해 PendingIntent 가 만들어지지 않았다", pendingIntent)

        val request = NotificationIntentContract.fromIntent(shadowOf(pendingIntent!!).savedIntent)

        assertEquals(NotificationEntrySource.DAILY, request?.source)
        assertEquals("occurrence-1", request?.occurrenceId)
    }

    private fun notificationIntent(
        isNotificationEntry: Boolean = true,
        source: String? = NotificationEntrySource.FCM.contractValue,
        occurrenceId: String? = "occurrence-1",
    ): Intent =
        Intent(Intent.ACTION_MAIN).apply {
            putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_ENTRY, isNotificationEntry)
            source?.let { putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE, it) }
            occurrenceId?.let { putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN, it) }
        }

    @Test
    fun `계약에 있는 목적지는 그대로 보존한다`() {
        NotificationDestination.entries.forEach { destination ->
            val request =
                resolveIntent(
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
                resolveIntent(
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
            resolveIntent(
                isNotificationEntry = true,
                rawSource = "fcm",
                occurrenceId = "occurrence-1",
                rawDestination = NotificationDestination.HOME.contractValue,
            )
        val toTimeLetter =
            resolveIntent(
                isNotificationEntry = true,
                rawSource = "fcm",
                occurrenceId = "occurrence-1",
                rawDestination = NotificationDestination.TIME_LETTER.contractValue,
            )

        assertEquals(toHome?.identityKey, toTimeLetter?.identityKey)
    }

    private fun resolveIntent(
        isNotificationEntry: Boolean,
        rawSource: String?,
        occurrenceId: String?,
        rawDestination: String?,
    ): NotificationEntryRequest? =
        NotificationIntentContract.fromIntent(
            notificationIntent(isNotificationEntry, rawSource, occurrenceId).apply {
                rawDestination?.let { putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_DESTINATION, it) }
            },
        )
}
