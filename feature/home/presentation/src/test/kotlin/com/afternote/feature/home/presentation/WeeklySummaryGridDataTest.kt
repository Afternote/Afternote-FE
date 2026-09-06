package com.afternote.feature.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 주간 요약 그리드가 자리표시자를 노출하지 않는지 (#562).
 *
 * 종전에는 파라미터 기본값이 자리표시자(`recordedCount = 7`)였고 호출부가 값을 넘기지
 * 않아, 이번 주 기록이 0건인 사용자에게도 **7** 이 그려졌다. 기본값을 없애는 것은
 * base(#207 리뷰)가 했고, 여기서는 **어떤 값이 무엇으로 그려지는지**를 고정한다 —
 * null 은 미상(대시), 0 은 확정값이라 숫자다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeeklySummaryGridDataTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `아직 모르면 자리표시자 숫자가 아니라 미상 표기가 나온다`() {
        // 값을 안 넘기는 경우는 base(#207 리뷰)에서 필수 파라미터로 막혔다 — 컴파일이 잡는다.
        // 여기서는 «조회 실패·조회 중» 인 null 이 무엇으로 그려지는지를 고정한다.
        composeRule.setContent {
            AfternoteTheme { WeeklySummaryGrid(recordedCount = null, onImageClick = {}, onCountCardClick = {}) }
        }

        composeRule.onNodeWithText("7").assertDoesNotExist()
        composeRule.onNodeWithText("–").assertIsDisplayed()
    }

    @Test
    fun `실제 기록 수를 그대로 그린다`() {
        composeRule.setContent {
            AfternoteTheme { WeeklySummaryGrid(recordedCount = 3, onImageClick = {}, onCountCardClick = {}) }
        }

        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `이번 주 기록이 0건이면 0으로 그린다`() {
        // «0건» 과 «못 불러옴» 은 다르다 — 0 은 확정값이라 숫자로 그린다.
        composeRule.setContent {
            AfternoteTheme { WeeklySummaryGrid(recordedCount = 0, onImageClick = {}, onCountCardClick = {}) }
        }

        composeRule.onNodeWithText("0").assertIsDisplayed()
    }
}
