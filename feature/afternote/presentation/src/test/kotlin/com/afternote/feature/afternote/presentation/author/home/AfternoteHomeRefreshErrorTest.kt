package com.afternote.feature.afternote.presentation.author.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.body.ListRefreshErrorBanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 목록이 남아 있는 상태의 새로고침 실패가 무음으로 끝나지 않는지에 대한 회귀 가드 (#705).
 *
 * 종전 [AfternoteHomeScreen] 은 `itemCount > 0` 이면 `LoadState.Error` 를 어디에도 싣지 않아,
 * 당겨 새로고침이 실패해도 화면은 이전 목록 그대로였다.
 *
 * 화면 전체가 아니라 판정 함수와 배너를 직접 검증한다 — `collectAsLazyPagingItems` 의 첫 상태가
 * Loading 이고 그것을 걷는 수집이 컴포지션 이펙트에서 돌아, 이 모듈처럼 Robolectric 클래스가
 * 누적된 뒤에는 화면 단위 단언이 실행 순서에 따라 갈린다([AfternoteHomeEmptyCopyTest] KDoc 참조).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteHomeRefreshErrorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `목록이 남아 있는 새로고침 실패는 배너로 알린다`() {
        assertTrue(shouldShowRefreshErrorBanner(errorState, itemCount = 3))
    }

    @Test
    fun `보여 줄 것이 없는 실패는 전면 오류가 맡으므로 배너를 그리지 않는다`() {
        assertFalse(shouldShowRefreshErrorBanner(errorState, itemCount = 0))
    }

    @Test
    fun `실패가 아니면 배너를 그리지 않는다`() {
        assertFalse(shouldShowRefreshErrorBanner(LoadState.Loading, itemCount = 3))
        assertFalse(
            shouldShowRefreshErrorBanner(LoadState.NotLoading(endOfPaginationReached = true), itemCount = 3),
        )
    }

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

    private val errorState get() = LoadState.Error(IOException("목록 새로고침 실패"))

    private fun string(resId: Int): String = composeRule.activity.getString(resId)
}
