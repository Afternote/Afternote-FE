package com.afternote.feature.afternote.presentation.editor.receiver

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 수신자 선택 화면 상태별 표시 계약 (#1427).
 *
 * 0건 상태의 시안 문구를 고정한다 (시안 4163:20979).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SelectReceiverScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `수신자 0건이면 시안 문구를 띄운다`() {
        setContent(SelectReceiverUiState(receivers = emptyList()))

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("수신자를 등록하고 쉽게 관리해 보세요.").assertIsDisplayed()
    }

    @Test
    fun `조회 실패는 다시 시도를 띄운다`() {
        setContent(SelectReceiverUiState(loadFailed = true))

        composeRule.onNodeWithText("수신자 목록을 불러오지 못했습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").assertIsDisplayed()
    }

    @Test
    fun `로딩 중에는 0건 문구를 먼저 띄우지 않는다`() {
        setContent(SelectReceiverUiState(isLoading = true, receivers = emptyList()))

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertDoesNotExist()
    }

    @Test
    fun `수신자가 있으면 0건 문구를 그리지 않는다`() {
        setContent(
            SelectReceiverUiState(receivers = listOf(AfternoteEditorReceiver(id = 1L, name = "김혜성", label = "아들"))),
        )

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertDoesNotExist()
    }

    private fun setContent(uiState: SelectReceiverUiState) {
        composeRule.setContent {
            AfternoteTheme {
                SelectReceiverScreen(
                    uiState = uiState,
                    onBackClick = {},
                    onReceiverToggle = {},
                    onRetryClick = {},
                    onConfirmClick = {},
                )
            }
        }
    }
}
