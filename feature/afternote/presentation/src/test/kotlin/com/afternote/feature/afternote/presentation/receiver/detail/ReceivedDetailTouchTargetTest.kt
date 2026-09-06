package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.testing.assertAccessibleClickTargets
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
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
}
