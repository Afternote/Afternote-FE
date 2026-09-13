package com.afternote.feature.setting.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.ReceiverListRouteContent
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListLoadState
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverListRouteContentTest {
    @get:Rule val composeRule = createComposeRule()
    private val receivers = listOf(ReceiverListItem(7L, "김수신", "가족"), ReceiverListItem(8L, "박친구", "친구"))

    @Test
    fun initialFailure_providesRetryAndBackWithoutShowingEmptySuccess() {
        var retries = 0
        var backs = 0
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListRouteContent(
                    uiState = ReceiverListUiState(loadState = ReceiverListLoadState.InitialFailure),
                    selectForDeliveryConditions = false,
                    onBackClick = { backs++ },
                    onRetry = { retries++ },
                    onConfirmClick = {},
                    onReceiverClick = {},
                )
            }
        }
        composeRule.onNodeWithText("수신자 목록을 불러오지 못했어요.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        assertEquals(1, retries)
        assertEquals(1, backs)
    }

    @Test
    fun refreshFailure_keepsManagementRowsAndRetryClickable() {
        var selected: Long? = null
        var retries = 0
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListRouteContent(
                    uiState = ReceiverListUiState(receivers, ReceiverListLoadState.RefreshFailure),
                    selectForDeliveryConditions = false,
                    onBackClick = {},
                    onRetry = { retries++ },
                    onConfirmClick = {},
                    onReceiverClick = { selected = it },
                )
            }
        }
        composeRule.onNodeWithText("박친구").performClick()
        composeRule.onNodeWithText("다시 시도").performClick()
        assertEquals(8L, selected)
        assertEquals(1, retries)
    }

    @Test
    fun resubscriptionLoading_preservesSearchAndSelectionButDisablesStaleConfirmation() {
        val state = mutableStateOf(ReceiverListUiState(receivers, ReceiverListLoadState.Ready))
        var confirmed: Long? = null
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListRouteContent(
                    uiState = state.value,
                    selectForDeliveryConditions = true,
                    onBackClick = {},
                    onRetry = {},
                    onConfirmClick = { confirmed = it.receiverId },
                    onReceiverClick = {},
                )
            }
        }
        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("김")
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))[0].performClick()
        composeRule.runOnIdle { state.value = ReceiverListUiState(loadState = ReceiverListLoadState.Loading) }
        composeRule.onNodeWithText("수신자 선택 완료하기").assertIsNotEnabled()
        composeRule.onNodeWithText("김수신").assertDoesNotExist()
        composeRule.runOnIdle { state.value = ReceiverListUiState(receivers, ReceiverListLoadState.Ready) }
        composeRule.onNodeWithText("김수신").assertIsDisplayed()
        composeRule.onNodeWithText("박친구").assertDoesNotExist()
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()
        assertEquals(7L, confirmed)
    }

    @Test
    fun selectionList_resubscriptionPreservesScrollPosition() = assertScrollPreserved(selecting = true)

    @Test
    fun managementList_resubscriptionPreservesScrollPosition() = assertScrollPreserved(selecting = false)

    private fun assertScrollPreserved(selecting: Boolean) {
        val longList = (0..49).map { ReceiverListItem(it.toLong(), "가수신%02d".format(it), "친구") }
        val state = mutableStateOf(ReceiverListUiState(longList, ReceiverListLoadState.Ready))
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListRouteContent(
                    uiState = state.value,
                    selectForDeliveryConditions = selecting,
                    onBackClick = {},
                    onRetry = {},
                    onConfirmClick = {},
                    onReceiverClick = {},
                )
            }
        }
        composeRule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(35)
        composeRule.onNodeWithText("가수신35").assertIsDisplayed()
        composeRule.runOnIdle { state.value = ReceiverListUiState(loadState = ReceiverListLoadState.Loading) }
        composeRule.onNodeWithText("가수신35").assertDoesNotExist()
        composeRule.runOnIdle { state.value = ReceiverListUiState(longList, ReceiverListLoadState.Ready) }
        composeRule.onNodeWithText("가수신35").assertIsDisplayed()
    }
}
