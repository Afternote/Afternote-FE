package com.afternote.core.ui.receiver

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ReceiverConsonantIndexTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var highlightedColor = Color.Unspecified

    @Test
    fun `마지막 한 명인 초성 탭은 목록 끝에 클램프되어도 하이라이트를 유지한다`() {
        setContent()

        composeRule.onNodeWithText("ㅎ").performTouchInput { click() }

        composeRule.onNodeWithText("홍마지막").assertIsDisplayed()
        assertHighlighted("ㅎ")
    }

    @Test
    fun `끝에서 클램프된 중간 초성은 탭을 유지하고 직접 스크롤하면 마지막 초성을 표시한다`() {
        setContent()

        composeRule.onNodeWithText("ㄴ").performTouchInput { click() }

        composeRule.onNodeWithText("홍마지막").assertIsDisplayed()
        assertHighlighted("ㄴ")

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex))
            .performTouchInput { swipeUp() }

        assertHighlighted("ㅎ")
    }

    @Test
    fun `검색 결과가 바뀌면 이전에 탭한 초성 대신 새 목록의 초성을 표시한다`() {
        setContent()
        composeRule.onNodeWithText("ㅎ").performTouchInput { click() }
        assertHighlighted("ㅎ")

        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("김")

        assertHighlighted("ㄱ")
    }

    @Test
    fun `검색 후 다시 탭한 초성을 현재 목록에 표시한다`() {
        setContent(leadingReceiverCount = 1)
        composeRule.onNodeWithText("ㅎ").performTouchInput { click() }
        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("수신")

        composeRule.onNodeWithText("ㄴ").performTouchInput { click() }

        assertHighlighted("ㄴ")
    }

    @Test
    fun `검색 후 초성 인덱스를 드래그하면 현재 목록에 표시한다`() {
        setContent(leadingReceiverCount = 1)
        composeRule.onNodeWithText("ㅎ").performTouchInput { click() }
        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("수신")
        val startY =
            composeRule
                .onNodeWithText("ㄱ")
                .fetchSemanticsNode()
                .boundsInRoot.center.y
        val endY =
            composeRule
                .onNodeWithText("ㄷ")
                .fetchSemanticsNode()
                .boundsInRoot.center.y

        composeRule.onNodeWithText("ㄱ").performTouchInput {
            swipe(start = center, end = center + Offset(0f, endY - startY))
        }

        assertHighlighted("ㄷ")
    }

    private fun setContent(leadingReceiverCount: Int = 30) {
        val receivers =
            List(leadingReceiverCount) { index ->
                ReceiverSelectItem(id = index.toLong(), name = "김수신$index", relation = "친구")
            } +
                listOf(
                    ReceiverSelectItem(id = leadingReceiverCount.toLong(), name = "나수신", relation = "친구"),
                    ReceiverSelectItem(id = leadingReceiverCount + 1L, name = "다수신", relation = "친구"),
                    ReceiverSelectItem(id = leadingReceiverCount + 2L, name = "홍마지막", relation = "친구"),
                )
        composeRule.setContent {
            AfternoteTheme {
                highlightedColor = AfternoteDesign.colors.gray9
                ReceiverSelectScreen(
                    receivers = receivers,
                    selectedReceiverId = null,
                    onReceiverToggle = {},
                    onBackClick = {},
                    onConfirmClick = {},
                )
            }
        }
    }

    private fun assertHighlighted(consonant: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText(consonant)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layouts)
            }
        val displayedColor =
            layouts
                .single()
                .layoutInput.style.color
        assertEquals(highlightedColor, displayedColor)
    }
}
