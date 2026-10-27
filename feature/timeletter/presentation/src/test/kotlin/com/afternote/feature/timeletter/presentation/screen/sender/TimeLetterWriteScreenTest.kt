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

        composeRule.onNodeWithText(SAVE_DRAFT_MESSAGE).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun `작성 중 뒤로가기에서 임시저장을 고르면 작성 내용으로 임시저장하고 직접 나가지 않는다`() {
        var backCount = 0
        val draftRequests = mutableListOf<String>()
        val titleState = TextFieldState()
        val dispatcher =
            setWriteScreen(
                titleState = titleState,
                onBackClick = { backCount++ },
                onDraftClick = { title, _ -> draftRequests += title },
            )

        composeRule.runOnIdle { titleState.edit { append("작성 중") } }
        composeRule.runOnIdle { dispatcher.onBackPressed() }
        composeRule.onNodeWithText(SAVE_DRAFT_MESSAGE).assertExists()
        composeRule.onNodeWithText("예").performClick()

        // 이탈은 임시저장 성공(savedAsDraft)이 맡는다 — 수신인 누락·저장 실패면 화면에 남아 오류를 보여야 한다.
        composeRule.onNodeWithText(SAVE_DRAFT_MESSAGE).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(listOf("작성 중"), draftRequests)
            assertEquals(0, backCount)
        }
    }

    @Test
    fun `임시저장을 거절하고 삭제를 확인하면 저장 없이 나간다`() {
        var backCount = 0
        var draftCount = 0
        var uiState by mutableStateOf(TimeLetterWriteUiState())
        setWriteScreen(
            uiState = { uiState },
            onBackClick = { backCount++ },
            onDraftClick = { _, _ -> draftCount++ },
        )

        composeRule.runOnIdle { uiState = uiState.copy(recipientIds = listOf(1L), sendAt = "2026-12-25") }
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeRule.onNodeWithText("아니요").performClick()
        composeRule.onNodeWithText(DISCARD_MESSAGE).assertExists()
        composeRule.onNodeWithText("예").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backCount)
            assertEquals(0, draftCount)
        }
    }

    @Test
    fun `삭제 확인에서 취소하면 작성 화면에 남고 내용을 지킨다`() {
        var backCount = 0
        val titleState = TextFieldState()
        val dispatcher = setWriteScreen(titleState = titleState, onBackClick = { backCount++ })

        composeRule.runOnIdle { titleState.edit { append("작성 중") } }
        composeRule.runOnIdle { dispatcher.onBackPressed() }
        composeRule.onNodeWithText("아니요").performClick()
        composeRule.onNodeWithText("취소").performClick()

        composeRule.onNodeWithText(DISCARD_MESSAGE).assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, backCount)
            assertEquals("작성 중", titleState.text.toString())
        }
    }

    @Test
    fun `예약 편지 수정 중 뒤로가기는 임시저장을 묻지 않고 수정 취소만 묻는다`() {
        var backCount = 0
        var draftCount = 0
        var uiState by mutableStateOf(TimeLetterWriteUiState(isLoadingEditingLetter = true))
        setWriteScreen(
            uiState = { uiState },
            onBackClick = { backCount++ },
            onDraftClick = { _, _ -> draftCount++ },
        )
        val loaded =
            TimeLetterWriteUiState(
                editingTimeLetterId = 1L,
                isEditingScheduledLetter = true,
                draftTitle = "기존 제목",
                recipientIds = listOf(1L),
                sendAt = "2026-12-25",
            )
        composeRule.runOnIdle { uiState = loaded }
        composeRule.waitForIdle()

        composeRule.runOnIdle { uiState = loaded.copy(sendAt = "2026-12-31") }
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.onNodeWithText(SAVE_DRAFT_MESSAGE).assertDoesNotExist()
        composeRule.onNodeWithText(DISCARD_EDIT_MESSAGE).assertExists()
        composeRule.onNodeWithText("예").performClick()

        composeRule.runOnIdle {
            assertEquals(1, backCount)
            assertEquals(0, draftCount)
        }
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

        composeRule.onNodeWithText(SAVE_DRAFT_MESSAGE).assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    private fun setWriteScreen(
        uiState: () -> TimeLetterWriteUiState = { TimeLetterWriteUiState() },
        titleState: TextFieldState = TextFieldState(),
        onBackClick: () -> Unit,
        onDraftClick: (String, Map<Long, String>) -> Unit = { _, _ -> },
    ): OnBackPressedDispatcher {
        lateinit var dispatcher: OnBackPressedDispatcher
        composeRule.setContent {
            dispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = uiState(),
                    titleState = titleState,
                    onBackClick = onBackClick,
                    onDraftClick = onDraftClick,
                )
            }
        }
        composeRule.waitForIdle()
        return dispatcher
    }

    private companion object {
        const val SAVE_DRAFT_MESSAGE = "작성 중인 내용을 임시저장할까요?"
        const val DISCARD_MESSAGE = "작성한 내용을 삭제할까요?"
        const val DISCARD_EDIT_MESSAGE = "수정한 내용을 취소할까요?"
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
