package com.afternote.feature.receiver.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentSlotState
import com.afternote.feature.receiver.presentation.deliveryverification.component.DocumentSlotCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 수신자 흐름 진입 컴포넌트의 클릭 타깃 가드.
 *
 * 서류 슬롯 카드(열람 신청)는 이름이 붙은 Button 역할이어야 하고, 최소 터치 크기를 밑돌지
 * 않아야 하며, 한 카드의 촬영·첨부 두 진입점이 서로 다른 콜백으로 갈려야 한다.
 *
 * 같은 규약을 거는 섹션 이동 버튼은 수신자 홈과 함께 `feature:home` 으로 옮겼다 (#1462) —
 * `ReceiverHomeTouchTargetTest` 에 있다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverFlowTouchTargetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `receiver document slot camera and attachment actions invoke distinct callbacks`() {
        var pickClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                Column {
                    DocumentSlotCard(
                        title = "Document",
                        slot = DocumentSlotState(),
                        onPickClick = { pickClicks++ },
                    )
                }
            }
        }

        composeRule.assertAccessibleClickTargets()
        val targets = composeRule.scanEnabledClickTargets()
        val attachTarget = targets.single { it.name == "서류 촬영 또는 파일 첨부" }
        assertEquals(Role.Button, targets.single { it.name == "촬영 또는 파일 첨부" }.role)
        assertEquals(Role.Button, attachTarget.role)
        assertEquals(43f, attachTarget.layoutWidth.value, 0.1f)
        assertEquals(43f, attachTarget.layoutHeight.value, 0.1f)
        assertFalse(attachTarget.isSmallerThan(MinimumTouchTargetSize))

        composeRule.onNodeWithText("촬영 또는 파일 첨부").performClick()
        composeRule.onAllNodesWithContentDescription("서류 촬영 또는 파일 첨부")[0].performClick()
        assertEquals(2, pickClicks)
    }
}
