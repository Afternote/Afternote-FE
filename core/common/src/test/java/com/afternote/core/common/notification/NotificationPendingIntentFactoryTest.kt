package com.afternote.core.common.notification

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * 알림 content [PendingIntent] 의 발생 구분 계약 회귀 가드.
 *
 * [PendingIntent] 의 동일성은 `Intent.filterEquals`(action·data·component…)로 판정하고 **extras 는
 * 보지 않는다**. 그래서 서로 다른 알림 발생이 같은 request code 를 재사용하면서도 alias 되지
 * 않으려면 action 이 갈려야 한다. 검증은 action 문자열 조립기가 아니라 팩토리가 실제로 내놓는
 * [PendingIntent] 로 한다 — 조립 규칙은 파일 내부 구현이고, 계약은 «다른 발생이면 다른
 * PendingIntent» 다 (#1672).
 */
@RunWith(RobolectricTestRunner::class)
class NotificationPendingIntentFactoryTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    fun registerLauncherActivity() {
        // 라이브러리 모듈 매니페스트에는 런처가 없다 — 팩토리가 쓰는 launcher Intent 를 심어 둔다.
        val launcher = ComponentName(context.packageName, "com.afternote.core.common.TestLauncherActivity")
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.addActivityIfNotPresent(launcher)
        shadowPackageManager.addIntentFilterForActivity(
            launcher,
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
    }

    @Test
    fun `같은 source와 occurrence token은 같은 PendingIntent 로 수렴한다`() {
        val first = create("fcm", "message-1")
        val second = create("fcm", "message-1")

        assertEquals(first, second)
    }

    @Test
    fun `source 또는 occurrence token이 다르면 다른 PendingIntent 다`() {
        val baseline = create("fcm", "message-1")

        assertNotEquals(baseline, create("daily", "message-1"))
        assertNotEquals(baseline, create("fcm", "message-2"))
    }

    @Test
    fun `Java hash가 충돌하는 occurrence token도 alias 되지 않는다`() {
        assertEquals("Aa".hashCode(), "BB".hashCode())

        assertNotEquals(create("fcm", "Aa"), create("fcm", "BB"))
    }

    @Test
    fun `source와 token의 경계가 달라지면 이어 붙인 문자가 같아도 alias 되지 않는다`() {
        assertNotEquals(create("a", "bc"), create("ab", "c"))
    }

    @Test
    fun `빈 identity 구성요소는 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            NotificationPendingIntentFactory.create(context, source = " ", occurrenceId = "token")
        }
        assertThrows(IllegalArgumentException::class.java) {
            NotificationPendingIntentFactory.create(context, source = "fcm", occurrenceId = "")
        }
    }

    @Test
    fun `진입 정보는 launcher Intent extras 로 실려 나간다`() {
        val intent = shadowOf(create("fcm", "message-1")).savedIntent

        assertTrue(intent.getBooleanExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_ENTRY, false))
        assertEquals("fcm", intent.getStringExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE))
        assertEquals(
            "message-1",
            intent.getStringExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN),
        )
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            intent.flags,
        )
    }

    private fun create(
        source: String,
        occurrenceId: String,
    ): PendingIntent =
        requireNotNull(
            NotificationPendingIntentFactory.create(context, source = source, occurrenceId = occurrenceId),
        ) { "런처 Intent 가 없어 PendingIntent 를 만들지 못했습니다" }
}
