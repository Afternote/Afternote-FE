package com.afternote.feature.setting.presentation

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.receiver.ReceiverManageScreen
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

    /** #556 — 0건에서 빈 화면이 아니라 안내 문구가 떠야 한다. */
    @Test
    fun emptyReceivers_showsEmptyGuide() {
        setReceiverContent(receivers = emptyList())

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("수신자를 등록하고 쉽게 관리해 보세요.").assertIsDisplayed()
    }

    /** #556 — 노션 2차 QA 6번이 짚은 등록 진입점 부재 축. 안내에서 바로 등록으로 갈 수 있어야 한다. */
    @Test
    fun emptyReceivers_registerButtonInvokesOnRegisterClick() {
        var registerCalls = 0
        setReceiverContent(receivers = emptyList(), onRegisterClick = { registerCalls += 1 })

        composeRule.onNodeWithText("수신자 등록하기").performClick()

        assertEquals(1, registerCalls)
    }

    @Test
    fun nonEmptyReceivers_hasNoEmptyGuide() {
        setReceiverContent()

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertDoesNotExist()
        composeRule.onNodeWithText("수신자 등록하기").assertDoesNotExist()
    }

    private fun setReceiverContent(
        receivers: List<ReceiverListItem> = this.receivers,
        onBackClick: () -> Unit = {},
        onReceiverClick: (Long) -> Unit = {},
        onRegisterClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverManageScreen(
                    receivers = receivers,
                    onBackClick = onBackClick,
                    onReceiverClick = onReceiverClick,
                    onRegisterClick = onRegisterClick,
                )
            }
        }
    }

    private companion object {
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
