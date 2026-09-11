package com.afternote.feature.setting.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CustomerCenterScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unavailableDialer_showsAlternativeContactMessage() {
        composeRule.setContent {
            AfternoteTheme {
                CustomerCenterScreen(
                    onBackClick = {},
                    onPhoneInquiryClick = { false },
                    onOneToOneInquiryClick = {},
                    onEmailInquiryClick = {},
                    onFaqClick = {},
                )
            }
        }

        composeRule.onNodeWithText("전화 문의").performClick()

        composeRule.onNodeWithText("전화 앱을 열 수 없어요. 이메일로 문의해 주세요.").assertIsDisplayed()
    }
}
