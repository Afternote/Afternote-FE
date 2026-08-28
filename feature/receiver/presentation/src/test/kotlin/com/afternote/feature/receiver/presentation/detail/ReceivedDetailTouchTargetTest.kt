package com.afternote.feature.receiver.presentation.detail

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
import com.afternote.feature.receiver.presentation.home.component.SectionGoButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceivedDetailTouchTargetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `password action exposes named button state transition`() {
        composeRule.setContent {
            AfternoteTheme {
                SocialNetworkReceivedDetailScreen(
                    onBackClick = {},
                    content =
                        ReceivedSocialNetworkDetailContent(
                            credentials = ReceivedAccountCredentialsUiModel(accountId = "id", password = "password"),
                        ),
                )
            }
        }

        composeRule.assertAccessibleClickTargets()
        val showTarget = composeRule.scanEnabledClickTargets().single { it.name == "표시" }
        assertEquals(Role.Button, showTarget.role)

        composeRule.onNodeWithText("표시").performClick()
        composeRule.waitForIdle()
        val hideTarget = composeRule.scanEnabledClickTargets().single { it.name == "숨김" }
        assertEquals(Role.Button, hideTarget.role)
        composeRule.onNodeWithText("password").assertExists()
    }

    @Test
    fun `receiver navigation field and attachment actions invoke distinct callbacks`() {
        var goClicks = 0
        var pickClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                Column {
                    SectionGoButton(text = "Go", onClick = { goClicks++ })
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
        val goTarget = targets.single { it.name == "Go" }
        val attachTarget = targets.single { it.name == "서류 촬영 또는 파일 첨부" }
        assertEquals(Role.Button, goTarget.role)
        assertEquals(Role.Button, targets.single { it.name == "촬영 또는 파일 첨부" }.role)
        assertEquals(Role.Button, attachTarget.role)
        assertEquals(43f, attachTarget.layoutWidth.value, 0.1f)
        assertEquals(43f, attachTarget.layoutHeight.value, 0.1f)
        assertFalse(goTarget.isSmallerThan(MinimumTouchTargetSize))
        assertFalse(attachTarget.isSmallerThan(MinimumTouchTargetSize))

        composeRule.onNodeWithText("Go").performClick()
        composeRule.onNodeWithText("촬영 또는 파일 첨부").performClick()
        composeRule.onAllNodesWithContentDescription("서류 촬영 또는 파일 첨부")[0].performClick()
        assertEquals(1, goClicks)
        assertEquals(2, pickClicks)
    }
}
