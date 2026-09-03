package com.afternote.feature.afternote.presentation.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorProcessingMethodDefaultsInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `추천 처리 방법 기본값 적용 뒤 기준선을 잡아 무변경 이탈을 바로 허용한다`() {
        var form by mutableStateOf(EditorFormState())
        var isProcessingMethodDefaultsInitializing by mutableStateOf(true)
        var backClicks = 0

        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = form,
                    onBackClick = { backClicks += 1 },
                    onSaveDraftClick = {},
                    onRegisterClick = {},
                    snackbarMessage = null,
                    onSnackbarMessageConsumed = {},
                    validationMessage = null,
                    onValidationMessageConsumed = {},
                    content = {},
                    shouldDeferBaselineCapture =
                        shouldDeferEditorBaselineCapture(
                            isPrefillLoading = false,
                            isProcessingMethodDefaultsInitializing = isProcessingMethodDefaultsInitializing,
                        ),
                )
            }
        }

        composeRule.runOnIdle {
            form =
                EditorFormState(
                    typeForm =
                        AfternoteTypeForm.Social(
                            processingMethods = listOf(ProcessingMethodItem(localId = 1, text = "계정 삭제")),
                        ),
                )
            isProcessingMethodDefaultsInitializing = false
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.runOnIdle { assertEquals(1, backClicks) }
    }
}
