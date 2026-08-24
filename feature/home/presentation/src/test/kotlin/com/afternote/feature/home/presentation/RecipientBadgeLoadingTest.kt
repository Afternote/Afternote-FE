package com.afternote.feature.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.badge.RecipientDesignationBadge
import com.afternote.core.ui.badge.RecipientDesignationBadgeState
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 조회 전 수신인 지정 배지 (#698).
 *
 * 로딩 분기가 `isRecipientDesignated = false` 를 넘겨, **아직 조회되지 않은 상태를
 * «미완료» 라는 결과로 단정**했다. 이미 지정한 사용자도 진입할 때마다 그 배지를 보고,
 * 배지가 탭 가능한 CTA 라 하지 않아도 될 행동을 유도했다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecipientBadgeLoadingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * 미결정 배지는 shimmer 가 무한 애니메이션이라 Compose 가 영원히 idle 이 되지 않는다.
     * 시계를 수동으로 돌려 한 프레임만 그린 뒤 읽는다.
     */
    private fun setUnknownBadge() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AfternoteTheme { RecipientDesignationBadge(state = RecipientDesignationBadgeState.Unknown) }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `조회 전에는 미완료로 확정하지 않는다`() {
        setUnknownBadge()

        composeRule.onNodeWithText("수신인 지정 미완료").assertDoesNotExist()
        composeRule.onNodeWithText("수신인 지정 완료").assertDoesNotExist()
    }

    @Test
    fun `조회 전 배지는 눌리지 않고 문구도 그리지 않는다`() {
        // 미결정 상태에서 CTA 를 노출하면 하지 않아도 될 행동을 유도한다. 문구 자체도
        // 그리지 않는다 — 시안에 없는 안내가 홈 첫 화면에 뜨면 그것대로 확정된 무언가로
        // 읽힌다. 읽을 것은 접근성으로만 준다.
        setUnknownBadge()

        composeRule.onNodeWithText("수신인 지정 여부 확인 중").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("수신인 지정 여부 확인 중")
            .assertHasNoClickAction()
    }

    @Test
    fun `홈 로딩 분기가 미완료 배지를 그리지 않는다`() {
        // 컴포넌트만 고쳐도 화면이 여전히 false 를 넘기면 증상이 그대로 남는다 — 배선까지 본다.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AfternoteTheme { HomeTabScreen(uiState = HomeTabUiState.Loading()) }
        }
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithText("수신인 지정 미완료").assertDoesNotExist()
    }

    @Test
    fun `조회가 끝나면 결과를 그대로 보여준다`() {
        composeRule.setContent {
            AfternoteTheme { RecipientDesignationBadge(state = RecipientDesignationBadgeState.Completed) }
        }

        composeRule.onNodeWithText("수신인 지정 완료").assertIsDisplayed()
    }
}
