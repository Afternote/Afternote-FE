package com.afternote.feature.afternote.presentation.editor

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * «등록» 액션이 진행 상태를 화면에 싣는지에 대한 회귀 가드 (#705).
 *
 * 종전에는 ViewModel 의 `isSaving` 이 Screen 에 전달되지 않아 저장 왕복 중에도 유휴 상태와 똑같이
 * 보였고, 연타가 그대로 통과했다. prefill 실패 중 잠금도 같은 배선을 쓴다 — 그쪽은 눌리면 빈 폼이
 * 기존 기록을 덮으므로 화면 차원의 차단이 필요하다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorSubmitGateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `등록이 열려 있으면 클릭이 저장으로 이어진다`() {
        var clicks = 0
        setEditor(isSubmitEnabled = true) { clicks += 1 }

        composeRule.onNodeWithText(submitLabel).assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun `저장 중이거나 prefill 을 못 읽었으면 등록 클릭이 통하지 않는다`() {
        var clicks = 0
        setEditor(isSubmitEnabled = false) { clicks += 1 }

        composeRule.onNodeWithText(submitLabel).assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals("잠긴 등록은 저장을 시작하지 않는다", 0, clicks) }
    }

    private val submitLabel: String
        get() = composeRule.activity.getString(R.string.afternote_editor_submit)

    private fun setEditor(
        isSubmitEnabled: Boolean,
        onRegisterClick: () -> Unit,
    ) {
        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = EditorFormState(),
                    onBackClick = {},
                    onRegisterClick = onRegisterClick,
                    snackbarMessage = null,
                    onSnackbarMessageConsumed = {},
                    validationMessage = null,
                    onValidationMessageConsumed = {},
                    content = {},
                    isSubmitEnabled = isSubmitEnabled,
                )
            }
        }
    }
}
