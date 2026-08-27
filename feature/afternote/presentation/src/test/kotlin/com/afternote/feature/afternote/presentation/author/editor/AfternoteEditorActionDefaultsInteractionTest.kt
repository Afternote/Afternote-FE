package com.afternote.feature.afternote.presentation.author.editor

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.navigation.isEditorBaselinePrefillLoading
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorActionDefaultsInteractionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `추천 템플릿 적용 뒤 기준선을 잡아 무변경 이탈을 바로 허용한다`() {
        var form by mutableStateOf(EditorFormState())
        var isActionTemplateInitializing by mutableStateOf(true)
        var backClicks = 0

        composeRule.setContent {
            AfternoteTheme {
                AfternoteEditorScreen(
                    form = form,
                    onBackClick = { backClicks += 1 },
                    onRegisterClick = {},
                    snackbarMessage = null,
                    onSnackbarMessageConsumed = {},
                    content = {},
                    isPrefillLoading =
                        isEditorBaselinePrefillLoading(
                            isPrefillLoading = false,
                            isActionTemplateInitializing = isActionTemplateInitializing,
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
            isActionTemplateInitializing = false
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.runOnIdle { assertEquals(1, backClicks) }
    }
}
