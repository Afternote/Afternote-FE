package com.afternote.feature.afternote.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.component.ListRefreshErrorBanner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 목록이 남아 있는 상태의 새로고침 실패가 무음으로 끝나지 않는지에 대한 회귀 가드 (#705).
 *
 * 종전 [AfternoteHomeScreen] 은 `itemCount > 0` 이면 `LoadState.Error` 를 어디에도 싣지 않아,
 * 당겨 새로고침이 실패해도 화면은 이전 목록 그대로였다.
 *
 * **여기서 잠그는 것은 배너 자체의 계약뿐이다.** 표시 여부를 정하는 판정은 화면 파일의 private
 * 함수라 테스트가 부르지 않는다 — 테스트가 부르려고 프로덕션 공개 범위를 넓히지 않는다(#1678).
 *
 * 화면을 그려서 세 갈래를 확인하는 방법은 **이 소스셋에서** 두 번 시도했고 CI 에서
 * `ComposeTimeoutException` 으로 무너졌다. `collectAsLazyPagingItems` 가 로드 상태를 화면까지
 * 옮기지 못한 채 끝나서 `waitUntil` 로 기다려도 배너가 나타나지 않는다 — Robolectric 클래스가
 * 누적되면 단언 시점까지 컴포지션 이펙트가 진행되지 않는다(#1443 이 실측한 기전).
 *
 * 그래서 **세 갈래는 계측으로 올라갔다.** `app` 의 `AfternoteHomeRefreshBannerAndroidTest` 가
 * 실기에서 실 Paging 을 태워 「실패+목록 있음 → 배너」·「실패+0건 → 전면 오류」·「실패 아님 →
 * 둘 다 아님」을 잠근다 (#1790).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeRefreshErrorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `배너는 실패 안내와 다시 시도를 함께 내놓는다`() {
        var retries = 0
        composeRule.setContent {
            AfternoteTheme { ListRefreshErrorBanner(onRetry = { retries += 1 }) }
        }

        composeRule.onNodeWithText(string(R.string.afternote_home_refresh_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.afternote_home_retry)).performClick()

        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)
}
