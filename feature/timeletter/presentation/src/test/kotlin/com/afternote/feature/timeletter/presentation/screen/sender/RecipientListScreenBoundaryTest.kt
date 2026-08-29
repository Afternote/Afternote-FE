package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** #556 — 등록 0건·검색 결과 0건에서 안내 없이 빈 화면이 되던 문제의 회귀 가드. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class RecipientListScreenBoundaryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyList_showsEmptyStateAndAllowsCancel() {
        var backCalls = 0
        var registerCalls = 0
        setRecipientContent(
            recipients = emptyList(),
            onBack = { backCalls += 1 },
            onRegister = { registerCalls += 1 },
        )

        composeRule.onNodeWithText("수신자 선택 완료하기").assertDoesNotExist()
        composeRule.onNodeWithText("등록된 수신자가 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("수신자 등록하기").performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertEquals(1, registerCalls)
        assertEquals(1, backCalls)
    }

    @Test
    fun searchNoResult_showsSearchEmptyStateWithoutRegisterCta() {
        setRecipientContent(recipients = defaultRecipients())

        composeRule.onNodeWithText("이름으로 검색하기").performTextInput("없는 이름")

        composeRule.onNodeWithText("일치하는 수신자가 없습니다.").assertIsDisplayed()
        composeRule.onNodeWithText("수신자 등록하기").assertDoesNotExist()
        composeRule.onNodeWithText("수신자 선택 완료하기").assertIsNotEnabled()
    }

    private fun setRecipientContent(
        recipients: List<ReceiverListItem>,
        onBack: () -> Unit = {},
        onConfirm: (List<ReceiverListItem>) -> Unit = {},
        onRegister: () -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                RecipientListContent(
                    recipients = recipients,
                    onBackClick = onBack,
                    onConfirmClick = onConfirm,
                    onRegisterClick = onRegister,
                )
            }
        }
    }

    private fun defaultRecipients() =
        listOf(
            ReceiverListItem(receiverId = 7L, name = "김수신", relation = "가족"),
            ReceiverListItem(receiverId = 11L, name = "박친구", relation = "친구"),
            ReceiverListItem(receiverId = 19L, name = "이동료", relation = "동료"),
        )
}
