package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.ReceiverListScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** #791 완료 전에도 보존돼야 하는 현재 설정 소비 화면의 선택 결과 계약. */
@RunWith(AndroidJUnit4::class)
class ReceiverSelectionAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private val receivers =
        listOf(
            ReceiverListItem(receiverId = 7L, name = "김수신", relation = "가족"),
            ReceiverListItem(receiverId = 11L, name = "박친구", relation = "친구"),
            ReceiverListItem(receiverId = 19L, name = "이동료", relation = "동료"),
        )

    @Test
    fun searchAndConfirm_returnsExactReceiverId() {
        var confirmed: ReceiverListItem? = null
        setReceiverContent { confirmed = it }

        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("김")
        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onNodeWithText("박친구").assertDoesNotExist()
        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(1)
            get(0).performClick()
        }
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        assertEquals(7L, confirmed?.receiverId)
        assertEquals("김수신", confirmed?.name)
        assertEquals("가족", confirmed?.relation)
    }

    @Test
    fun changingSingleSelection_returnsOnlyLatestReceiver() {
        var confirmed: ReceiverListItem? = null
        setReceiverContent { confirmed = it }

        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(3)
            get(1).performClick()
            get(0).performClick()
        }
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        assertEquals(7L, confirmed?.receiverId)
    }

    @Test
    fun confirmWithoutSelection_doesNotEmitResult() {
        var callbackCount = 0
        setReceiverContent { callbackCount += 1 }

        composeRule
            .onNodeWithText("수신자 선택 완료하기")
            .assertIsNotEnabled()

        assertEquals(0, callbackCount)
    }

    @Test
    fun selectingSameReceiverTwice_disablesConfirmationAndDoesNotEmitResult() {
        var callbackCount = 0
        setReceiverContent { callbackCount += 1 }

        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(3)
            get(0).performClick()
            get(0).performClick()
        }
        composeRule
            .onNodeWithText("수신자 선택 완료하기")
            .assertIsNotEnabled()

        assertEquals(0, callbackCount)
    }

    @Test
    fun selectionHiddenBySearch_isRetainedAndConfirmsExactOriginalReceiver() {
        var confirmed: ReceiverListItem? = null
        setReceiverContent { confirmed = it }

        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(3)
            get(1).performClick()
        }
        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("김")
        composeRule.onNodeWithText("박친구").assertDoesNotExist()
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()

        assertEquals(receivers[1], confirmed)
    }

    private fun setReceiverContent(onConfirm: (ReceiverListItem) -> Unit) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListScreen(
                    receivers = receivers,
                    onBackClick = {},
                    onConfirmClick = onConfirm,
                )
            }
        }
    }

    private companion object {
        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    }
}
