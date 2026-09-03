package com.afternote.feature.afternote.presentation.author.editor.receiver.select

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 수신자 선택 화면 상태별 표시 계약 (#1427).
 *
 * 0건 상태의 시안 문구·일러스트를 고정한다 (시안 4163:20979). 등록 CTA 와 진입 콜백은
 * 배선을 붙이는 #1793 이 함께 되살린다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// 빈 상태는 화면 높이를 다 쓰는 레이아웃이라 기본 qualifiers 로는 CTA 가 잘려 «표시 안 됨» 이 된다.
// 시안 4163:20979 과 같은 급의 기기(360×800dp)에서 스크롤 없이 보이는지를 고정한다.
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class SelectReceiverScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `수신자 0건이면 시안 문구를 띄운다`() {
        setContent(SelectReceiverUiState(receivers = emptyList()))

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("수신자를 등록하고 쉽게 관리해 보세요.").assertIsDisplayed()
        // 등록 CTA 는 진입 배선이 붙는 #1793 에서 다시 켠다 — 지금 그리면 갈 곳 없는 버튼이 된다.
        composeRule.onNodeWithText("수신자 등록하기").assertDoesNotExist()
    }

    @Test
    fun `조회 실패는 등록 CTA 가 아니라 다시 시도를 띄운다`() {
        setContent(SelectReceiverUiState(loadFailed = true))

        composeRule.onNodeWithText("수신자 목록을 불러오지 못했습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("다시 시도").assertIsDisplayed()
        composeRule.onNodeWithText("수신자 등록하기").assertDoesNotExist()
    }

    @Test
    fun `로딩 중에는 0건 문구를 먼저 띄우지 않는다`() {
        setContent(SelectReceiverUiState(isLoading = true, receivers = emptyList()))

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertDoesNotExist()
        composeRule.onNodeWithText("수신자 등록하기").assertDoesNotExist()
    }

    @Test
    fun `수신자가 있으면 0건 문구도 등록 진입점도 그리지 않는다`() {
        setContent(
            SelectReceiverUiState(receivers = listOf(AfternoteEditorReceiver(id = 1L, name = "김혜성", label = "아들"))),
        )

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertDoesNotExist()
        composeRule.onNodeWithText("수신자 등록하기").assertDoesNotExist()
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
