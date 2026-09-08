package com.afternote.feature.setting.presentation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.ReceiverManageScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** #631 — 설정의 수신자 목록은 선택 화면이 아니라 관리 화면으로 동작해야 한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverManageScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val receivers =
        listOf(
            ReceiverListItem(receiverId = 7L, name = "김수신", relation = "가족"),
            ReceiverListItem(receiverId = 11L, name = "박친구", relation = "친구"),
        )

    @Test
    fun tappingRow_navigatesWithTappedReceiverId() {
        var clickedId: Long? = null
        setReceiverContent(onReceiverClick = { clickedId = it })

        composeRule.onNodeWithText("박친구").performClick()

        assertEquals(11L, clickedId)
    }

    @Test
    fun screen_hasNoSelectionUi() {
        setReceiverContent()

        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(0)
        composeRule.onNodeWithText("수신자 선택 완료하기").assertDoesNotExist()
    }

    @Test
    fun backClick_invokesOnBackClick() {
        var backCalls = 0
        setReceiverContent(onBackClick = { backCalls += 1 })

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertEquals(1, backCalls)
    }

    private fun setReceiverContent(
        onBackClick: () -> Unit = {},
        onReceiverClick: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverManageScreen(
                    receivers = receivers,
                    onBackClick = onBackClick,
                    onReceiverClick = onReceiverClick,
                )
            }
        }
    }

    private companion object {
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
