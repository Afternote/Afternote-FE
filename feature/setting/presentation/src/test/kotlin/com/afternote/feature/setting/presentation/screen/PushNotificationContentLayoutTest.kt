package com.afternote.feature.setting.presentation.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PushNotificationContentLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `알림 항목 세 개는 읽고 누를 수 있도록 세로로 분리한다`() {
        composeRule.setContent {
            AfternoteTheme {
                PushNotificationContent(
                    uiState = PushNotificationUiState(isLoading = false),
                    onBack = {},
                    onNewsletterToggle = {},
                    onMindRecordToggle = {},
                    onAfternoteToggle = {},
                    onRetry = {},
                )
            }
        }

        val bounds =
            listOf("타임레터", "마음의 기록", "애프터노트").map { label ->
                composeRule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
            }
        bounds.zipWithNext().forEach { (first, second) ->
            assertTrue("알림 항목이 겹칩니다: $first / $second", first.bottom <= second.top)
        }
    }
}
