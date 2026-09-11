package com.afternote.feature.setting.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.FaqScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val QUESTION = "비밀번호를 잊어버렸어요."
private const val ANSWER = "로그인 화면의 '비밀번호 찾기'를 통해 재설정할 수 있습니다."

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class FaqScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun answer_isHiddenUntilQuestionRowIsClicked_thenToggleHidesItAgain() {
        composeRule.setContent {
            AfternoteTheme {
                FaqScreen(onBackClick = {})
            }
        }

        composeRule.onAllNodesWithText(ANSWER).assertCountEquals(0)

        composeRule.onNodeWithText(QUESTION).performClick()
        composeRule.onNodeWithText(ANSWER).assertIsDisplayed()

        composeRule.onNodeWithText(QUESTION).performClick()
        composeRule.onAllNodesWithText(ANSWER).assertCountEquals(0)
    }

    @Test
    fun backClick_invokesOnBackClick() {
        var backClicked = false
        composeRule.setContent {
            AfternoteTheme {
                FaqScreen(onBackClick = { backClicked = true })
            }
        }

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertTrue(backClicked)
    }
}
