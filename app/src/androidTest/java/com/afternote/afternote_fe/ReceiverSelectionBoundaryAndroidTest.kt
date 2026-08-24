package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.ReceiverListScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** #791 공용 API 전에도 설정 selector가 지켜야 하는 표현 가능한 경계만 검증한다. */
@RunWith(AndroidJUnit4::class)
class ReceiverSelectionBoundaryAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun emptyList_disablesConfirmationAndAllowsCancel() {
        var backCalls = 0
        var confirmCalls = 0
        setReceiverContent(
            receivers = emptyList(),
            onBack = { backCalls += 1 },
            onConfirm = { confirmCalls += 1 },
        )

        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(0)
        composeRule.onNodeWithText("수신자 선택 완료하기").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertEquals(1, backCalls)
        assertEquals(0, confirmCalls)
    }

    @Test
    fun searchNoResult_thenClear_restoresTheSameReceiverSet() {
        val receivers = defaultReceivers()
        setReceiverContent(receivers = receivers)

        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("없는 이름")
        receivers.forEach { receiver ->
            composeRule.onNodeWithText(receiver.name).assertDoesNotExist()
        }
        composeRule.onNodeWithText("수신자 선택 완료하기").assertIsNotEnabled()

        composeRule.onNodeWithText("없는 이름").performTextClearance()
        receivers.forEach { receiver ->
            composeRule.onNodeWithText(receiver.name).assertIsDisplayed()
        }
    }

    @Test
    fun consonantIndexReturnsToTargetGroup_andBackDoesNotEmitSelection() {
        val receivers =
            buildList {
                repeat(10) { index ->
                    add(ReceiverListItem(receiverId = index.toLong(), name = "가$index", relation = "가족"))
                }
                repeat(10) { index ->
                    add(ReceiverListItem(receiverId = (100 + index).toLong(), name = "히$index", relation = "친구"))
                }
            }
        var backCalls = 0
        var confirmed: ReceiverListItem? = null
        setReceiverContent(
            receivers = receivers,
            onBack = { backCalls += 1 },
            onConfirm = { confirmed = it },
        )

        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(receivers.lastIndex)
        composeRule.onNodeWithText("히9").assertIsDisplayed()
        composeRule.onNodeWithText("ㄱ").performTouchInput { click() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(checkboxMatcher).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("가0").assertIsDisplayed()

        composeRule.onAllNodes(checkboxMatcher)[0].performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertEquals(1, backCalls)
        assertNull(confirmed)
    }

    private fun setReceiverContent(
        receivers: List<ReceiverListItem>,
        onBack: () -> Unit = {},
        onConfirm: (ReceiverListItem) -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListScreen(
                    receivers = receivers,
                    onBackClick = onBack,
                    onConfirmClick = onConfirm,
                )
            }
        }
    }

    private fun defaultReceivers() =
        listOf(
            ReceiverListItem(receiverId = 7L, name = "김수신", relation = "가족"),
            ReceiverListItem(receiverId = 11L, name = "박친구", relation = "친구"),
            ReceiverListItem(receiverId = 19L, name = "이동료", relation = "동료"),
        )

    private companion object {
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
