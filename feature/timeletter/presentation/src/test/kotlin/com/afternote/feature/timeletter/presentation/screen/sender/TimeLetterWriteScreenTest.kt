package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TimeLetterWriteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `editing title is populated after letter loading completes`() {
        var uiState by
            mutableStateOf(
                TimeLetterWriteUiState(isLoadingEditingLetter = true),
            )
        val titleState = TextFieldState()

        composeRule.setContent {
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = uiState,
                    titleState = titleState,
                )
            }
        }

        composeRule.runOnIdle {
            assertEquals("", titleState.text.toString())
            uiState =
                TimeLetterWriteUiState(
                    editingTimeLetterId = 1L,
                    draftTitle = "existing title",
                )
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals("existing title", titleState.text.toString())
        }
    }

    @Test
    fun `voice recorder keeps audio file picker available`() {
        composeRule.setContent {
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = TimeLetterWriteUiState(showVoiceRecorder = true),
                    titleState = TextFieldState(),
                )
            }
        }

        composeRule
            .onNodeWithText("기기에서 음성 파일 선택")
            .assertExists()
    }
}
