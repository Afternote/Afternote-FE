package com.afternote.feature.setting.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.receiver.ReceiverListLoadState
import com.afternote.feature.setting.presentation.receiver.ReceiverListRouteContent
import com.afternote.feature.setting.presentation.receiver.ReceiverListUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 설정 수신자 목록 라우트가 조회 상태를 관리·선택 두 화면에 옮기는 규칙 (#1281).
 *
 * 조회 중·실패를 성공한 0건으로 그리면 등록된 수신자가 없다는 거짓 안내와 등록 버튼이 뜬다. 그 두 줄이
 * 실제 0건일 때만 나오는지, 행이 있을 때는 조회 중·갱신 실패에도 행과 선택이 남는지를 본다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverListRouteContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var retryCalls = 0
    private var confirmed: ReceiverListItem? = null
    private var clickedReceiverId: Long? = null

    @Test
    fun manage_firstLoading_showsProgressWithoutEmptyGuideOrRegisterButton() {
        setContent(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading), selectForDeliveryConditions = false)

        composeRule.onAllNodes(progressMatcher).assertCountEquals(1)
        composeRule.onNodeWithText(EMPTY_GUIDE).assertDoesNotExist()
        composeRule.onNodeWithText(REGISTER).assertDoesNotExist()
    }

    @Test
    fun manage_failureWithoutRows_showsFailureAndRetryInsteadOfEmptyGuide() {
        setContent(ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure), selectForDeliveryConditions = false)

        composeRule.onNodeWithText(LOAD_FAILED).assertIsDisplayed()
        composeRule.onNodeWithText(EMPTY_GUIDE).assertDoesNotExist()
        composeRule.onNodeWithText(REGISTER).assertDoesNotExist()
        composeRule.onNodeWithText(RETRY).performClick()

        assertEquals(1, retryCalls)
    }

    @Test
    fun manage_readyWithZeroReceivers_keepsEmptyGuideAndRegisterButton() {
        setContent(ReceiverListUiState(emptyList(), ReceiverListLoadState.Ready), selectForDeliveryConditions = false)

        composeRule.onNodeWithText(EMPTY_GUIDE).assertIsDisplayed()
        composeRule.onNodeWithText(REGISTER).assertIsDisplayed()
        composeRule.onNodeWithText(LOAD_FAILED).assertDoesNotExist()
    }

    @Test
    fun manage_loadingWithRows_keepsRowsWithoutProgressOrBanner() {
        setContent(ReceiverListUiState(receivers, ReceiverListLoadState.Loading), selectForDeliveryConditions = false)

        composeRule.onNodeWithText("박친구").assertIsDisplayed()
        composeRule.onAllNodes(progressMatcher).assertCountEquals(0)
        composeRule.onNodeWithText(REFRESH_FAILED).assertDoesNotExist()
    }

    @Test
    fun manage_refreshFailure_keepsRowsAndShowsBannerRetry() {
        setContent(ReceiverListUiState(receivers, ReceiverListLoadState.RefreshFailure), selectForDeliveryConditions = false)

        composeRule.onNodeWithText(REFRESH_FAILED).assertIsDisplayed()
        composeRule.onNodeWithText(LOAD_FAILED).assertDoesNotExist()
        composeRule.onNodeWithText(RETRY).performClick()
        composeRule.onNodeWithText("박친구").performClick()

        assertEquals(1, retryCalls)
        assertEquals(11L, clickedReceiverId)
    }

    @Test
    fun select_firstLoading_replacesListWithProgress() {
        setContent(ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading), selectForDeliveryConditions = true)

        composeRule.onAllNodes(progressMatcher).assertCountEquals(1)
        composeRule.onAllNodes(checkboxMatcher).assertCountEquals(0)
    }

    @Test
    fun select_failureWithoutRows_showsFailureAndRetry() {
        setContent(ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure), selectForDeliveryConditions = true)

        composeRule.onNodeWithText(LOAD_FAILED).assertIsDisplayed()
        composeRule.onNodeWithText(RETRY).performClick()

        assertEquals(1, retryCalls)
    }

    /** 검색·선택을 마친 뒤 다시 불러오기가 돌고 실패해도 같은 화면에서 그 선택으로 완료할 수 있어야 한다. */
    @Test
    fun select_refreshLoadingThenFailure_keepsSearchAndSelection() {
        var uiState by mutableStateOf(ReceiverListUiState(receivers, ReceiverListLoadState.Ready))
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListRouteContent(
                    uiState = uiState,
                    selectForDeliveryConditions = true,
                    onBackClick = {},
                    onRetryClick = { retryCalls += 1 },
                    onConfirmClick = { confirmed = it },
                    onReceiverClick = {},
                    onRegisterClick = {},
                )
            }
        }
        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("박")
        composeRule.onAllNodes(checkboxMatcher).run {
            assertCountEquals(1)
            get(0).performClick()
        }

        uiState = ReceiverListUiState(receivers, ReceiverListLoadState.Loading)
        composeRule.waitForIdle()
        uiState = ReceiverListUiState(receivers, ReceiverListLoadState.RefreshFailure)
        composeRule.waitForIdle()

        composeRule.onNodeWithText("박친구").assertIsDisplayed()
        composeRule.onNodeWithText("김수신").assertDoesNotExist()
        composeRule.onNodeWithText("수신자 선택 완료하기").performClick()
        assertEquals(receivers[1], confirmed)
    }

    private fun setContent(
        uiState: ReceiverListUiState,
        selectForDeliveryConditions: Boolean,
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverListRouteContent(
                    uiState = uiState,
                    selectForDeliveryConditions = selectForDeliveryConditions,
                    onBackClick = {},
                    onRetryClick = { retryCalls += 1 },
                    onConfirmClick = { confirmed = it },
                    onReceiverClick = { clickedReceiverId = it },
                    onRegisterClick = {},
                )
            }
        }
    }

    private val receivers =
        listOf(
            ReceiverListItem(receiverId = 7L, name = "김수신", relation = "가족"),
            ReceiverListItem(receiverId = 11L, name = "박친구", relation = "친구"),
        )

    private companion object {
        const val EMPTY_GUIDE = "등록된 수신자가 없습니다."
        const val REGISTER = "수신자 등록하기"
        const val LOAD_FAILED = "수신자 목록을 불러오지 못했습니다."
        const val REFRESH_FAILED = "목록을 새로 불러오지 못했습니다. 지금 보이는 내용이 최신이 아닐 수 있어요."
        const val RETRY = "다시 시도"

        val checkboxMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        val progressMatcher =
            SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)
    }
}
