package com.afternote.feature.afternote.presentation.detail

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteDetailContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `실제 상세 상태가 삭제 진행 표시를 올리고 완료하면 제거한다`() {
        var state by mutableStateOf<AfternoteDetailUiState>(
            AfternoteDetailUiState.Success(
                detailId = 73L,
                contentUiModel = DetailContentUiModel.Memorial(MemorialDetailContent()),
                authorDisplayName = "서영",
                isDeleting = true,
            ),
        )
        val intents = mutableListOf<AfternoteDetailIntent>()
        var backClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                AfternoteDetailContent(
                    uiState = state,
                    onIntent = intents::add,
                    snackbarHostState = SnackbarHostState(),
                    onNavigateBack = { backClicks++ },
                    onNavigateToEditor = { _, _ -> },
                    onVideoClick = {},
                )
            }
        }
        val deleting = composeRule.activity.getString(R.string.afternote_detail_deleting)
        composeRule.onNodeWithText("추억 노트에 대한\n서영님의 기록").assertExists()
        composeRule.onNodeWithContentDescription(deleting).assertIsDisplayed()
        val back = composeRule.activity.getString(com.afternote.core.ui.R.string.core_ui_content_description_back)
        composeRule.onNodeWithContentDescription(back).performTouchInput { click() }
        composeRule.runOnIdle {
            assertEquals(emptyList<AfternoteDetailIntent>(), intents)
            assertEquals(0, backClicks)
            state = (state as AfternoteDetailUiState.Success).copy(isDeleting = false)
        }
        composeRule.onNodeWithContentDescription(deleting).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(back).performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(1, backClicks) }
    }
}
