package com.afternote.afternote_fe.deeplink

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import com.afternote.core.common.deeplink.AppLinkRejectionReason
import com.afternote.core.common.deeplink.AppLinkResolution
import com.afternote.core.common.deeplink.NavigationTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 링크 진입 Intent 해석 계약의 회귀 기준 (#924).
 *
 * `MainActivity` 가 실제로 부르는 `fromIntent(Intent)` 에 진짜 [Intent] 를 넣는다 — 그래야 액션과
 * data 를 꺼내는 방식까지 계약에 포함된다. 문자열만 파서에 넣어 보면 「VIEW 가 아닌 진입을
 * 링크로 오인하는」 회귀가 지나간다.
 *
 * cold start 와 warm `onNewIntent` 는 **같은 함수**를 지난다. 그래서 여기서 한 번 잠그면 두 경계가
 * 같은 목적지로 가는 것이 함께 잠긴다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AppLinkIntentContractTest {
    @Test
    fun `런처 진입과 알림 진입은 링크가 아니다`() {
        assertNull(AppLinkIntentContract.fromIntent(Intent(Intent.ACTION_MAIN)))
        assertNull(AppLinkIntentContract.fromIntent(Intent("com.afternote.NOTIFICATION_ENTRY")))
    }

    @Test
    fun `계약 표의 링크는 목적지로 해석된다`() {
        val rows =
            mapOf(
                "https://afternote.kro.kr/" to NavigationTarget.Home,
                "https://afternote.kro.kr/afternote" to NavigationTarget.AfternoteHome,
                "https://afternote.kro.kr/timeletter/12" to NavigationTarget.TimeLetterDetail(12L),
                "https://afternote.kro.kr/mindrecord/daily-question" to NavigationTarget.DailyQuestionCompose,
                "https://afternote.kro.kr/settings/notification" to NavigationTarget.NotificationSettings,
            )

        rows.forEach { (link, expected) ->
            assertEquals(link, AppLinkResolution.Resolved(expected), AppLinkIntentContract.fromIntent(viewIntent(link)))
        }
    }

    @Test
    fun `계약 밖 링크는 사유와 함께 거절된다`() {
        val resolution = AppLinkIntentContract.fromIntent(viewIntent("https://afternote.kro.kr/unknown"))

        assertEquals(AppLinkResolution.Rejected(AppLinkRejectionReason.UNKNOWN_PATH), resolution)
        assertEquals(NavigationTarget.Home, resolution?.target)
    }

    @Test
    fun `data 없는 VIEW 는 무시가 아니라 거절이다`() {
        assertEquals(
            AppLinkResolution.Rejected(AppLinkRejectionReason.MALFORMED_URI),
            AppLinkIntentContract.fromIntent(Intent(Intent.ACTION_VIEW)),
        )
    }

    private fun viewIntent(link: String): Intent = Intent(Intent.ACTION_VIEW, link.toUri())
}
