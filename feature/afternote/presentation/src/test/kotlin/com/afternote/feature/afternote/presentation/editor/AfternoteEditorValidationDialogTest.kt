package com.afternote.feature.afternote.presentation.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorValidationDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `검증 실패는 확인 팝업으로 표시하고 확인 시 소비한다`() {
        var consumed = 0
        val message = "서비스명: 애프터노트를 등록하려면 서비스를 먼저 선택해 주세요."
        var validationMessage by mutableStateOf<String?>(message)

        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = EditorFormState(),
                    onBackClick = {},
                    onRegisterClick = {},
                    snackbarMessage = null,
                    onSnackbarMessageConsumed = {},
                    content = {},
                    validationMessage = validationMessage,
                    onValidationMessageConsumed = {
                        consumed += 1
                        validationMessage = null
                    },
                )
            }
        }

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText("확인").performClick()

        composeRule.runOnIdle { assertEquals(1, consumed) }
    }
}
