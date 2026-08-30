package com.afternote.feature.afternote.presentation.author.editor.receiver.select

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 수신자 선택 화면 상태별 표시 계약 (#1427).
 *
 * 0건 상태에서 등록 진입점이 사라지면 신규 사용자가 첫 애프터노트 작성에서 막힌다 —
 * 문구·CTA 존재와 진입 콜백을 시안 4163:20979 기준으로 고정한다.
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
    fun `수신자 0건이면 시안 문구와 등록 CTA 를 띄운다`() {
        setContent(SelectReceiverUiState(receivers = emptyList()))

        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("수신자를 등록하고 쉽게 관리해 보세요.").assertIsDisplayed()
        composeRule.onNodeWithText("수신자 등록하기").assertIsDisplayed()
    }

    @Test
    fun `0건 CTA 를 누르면 등록 진입 콜백이 나간다`() {
        var registerClicks = 0
        setContent(
            SelectReceiverUiState(receivers = emptyList()),
            onRegisterReceiverClick = { registerClicks += 1 },
        )

        composeRule.onNodeWithText("수신자 등록하기").performClick()

        assertEquals(1, registerClicks)
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
    fun `수신자가 있으면 목록 하단 등록 행으로 같은 진입점을 낸다`() {
        var registerClicks = 0
        setContent(
            SelectReceiverUiState(receivers = listOf(AfternoteEditorReceiver(id = 1L, name = "김혜성", label = "아들"))),
            onRegisterReceiverClick = { registerClicks += 1 },
        )

        // 0건 CTA 는 사라지고 목록 하단 행 하나만 남는다 — 진입점이 둘이 되면 안 된다.
        composeRule.onNodeWithText("수신자 등록하기").assertDoesNotExist()
        composeRule.onNodeWithText("새 수신자 등록").performClick()

        assertEquals(1, registerClicks)
    }

    private fun setContent(
        uiState: SelectReceiverUiState,
        onRegisterReceiverClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                SelectReceiverScreen(
                    uiState = uiState,
                    onBackClick = {},
                    onReceiverToggle = {},
                    onRetryClick = {},
                    onConfirmClick = {},
                    onRegisterReceiverClick = onRegisterReceiverClick,
                )
            }
        }
    }
}
