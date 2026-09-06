package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 수신자 홈 섹션 이동 버튼의 클릭 타깃 가드.
 *
 * 종전에는 `feature:receiver` 의 `ReceiverFlowTouchTargetTest` 가 서류 슬롯 카드와 한 테스트에서
 * 같은 규약을 걸었다. 수신자 홈이 `feature:home` 으로 옮겨지며(#1462) 두 컴포넌트가 다른
 * 모듈이 됐으므로 단언을 그대로 둔 채 이쪽 몫만 옮겼다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverHomeTouchTargetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `section go button is a named button target that meets the minimum size`() {
        var goClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                SectionGoButton(text = "Go", onClick = { goClicks++ })
            }
        }

        composeRule.assertAccessibleClickTargets()
        val goTarget = composeRule.scanEnabledClickTargets().single { it.name == "Go" }
        assertEquals(Role.Button, goTarget.role)
        assertFalse(goTarget.isSmallerThan(MinimumTouchTargetSize))

        composeRule.onNodeWithText("Go").performClick()
        assertEquals(1, goClicks)
    }
}
