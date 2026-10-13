package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    fun `변경 없이 뒤로가면 팝업 없이 바로 나간다`() {
        var backCount = 0
        setWriteScreen(onBackClick = { backCount++ })

        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.onNodeWithText("나가기").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun `작성 중 시스템 뒤로가기는 이탈 확인 팝업을 띄우고 계속 작성하면 화면에 남는다`() {
        var backCount = 0
        val titleState = TextFieldState()
        val dispatcher = setWriteScreen(titleState = titleState, onBackClick = { backCount++ })

        composeRule.runOnIdle { titleState.edit { append("작성 중") } }
        composeRule.runOnIdle { dispatcher.onBackPressed() }

        composeRule.onNodeWithText("나가기").assertExists()
        composeRule.onNodeWithText("계속 작성").performClick()

        composeRule.onNodeWithText("나가기").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, backCount)
            assertEquals("작성 중", titleState.text.toString())
        }
    }

    @Test
    fun `작성 중 상단바 뒤로가기에서 나가기를 고르면 나간다`() {
        var backCount = 0
        var uiState by mutableStateOf(TimeLetterWriteUiState())
        setWriteScreen(uiState = { uiState }, onBackClick = { backCount++ })

        composeRule.runOnIdle { uiState = uiState.copy(recipientIds = listOf(1L), sendAt = "2026-12-25") }
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeRule.onNodeWithText("나가기").performClick()

        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun `수정 모드에서 불러온 내용을 바꾸지 않고 나가면 팝업을 띄우지 않는다`() {
        var backCount = 0
        var uiState by mutableStateOf(TimeLetterWriteUiState(isLoadingEditingLetter = true))
        setWriteScreen(uiState = { uiState }, onBackClick = { backCount++ })

        composeRule.runOnIdle {
            uiState =
                TimeLetterWriteUiState(
                    editingTimeLetterId = 1L,
                    draftTitle = "기존 제목",
                    draftTextContents = mapOf(0L to "기존 내용"),
                    recipientIds = listOf(1L),
                    sendAt = "2026-12-25",
                )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.onNodeWithText("나가기").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    private fun setWriteScreen(
        uiState: () -> TimeLetterWriteUiState = { TimeLetterWriteUiState() },
        titleState: TextFieldState = TextFieldState(),
        onBackClick: () -> Unit,
    ): OnBackPressedDispatcher {
        lateinit var dispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            dispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = uiState(),
                    titleState = titleState,
                    onBackClick = onBackClick,
                )
            }
        }
        composeRule.waitForIdle()
        return dispatcher
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
