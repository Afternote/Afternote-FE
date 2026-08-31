package com.afternote.afternote_fe

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteUiState
import com.afternote.feature.timeletter.presentation.viewmodel.VoiceRecordingState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 녹음 중 화면 이탈(`ON_STOP`)이 실제 Activity 생명주기 위에서 녹음을 폐기하는지 검증한다.
 * Robolectric 유닛 테스트(`TimeLetterWriteScreenTest`)는 호스트 Activity 를 직접 STOP
 * 시키지 못해 이 경로를 지나지 않는다 (#437 리뷰).
 */
@RunWith(AndroidJUnit4::class)
class VoiceRecordingLifecycleAndroidTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appBackgrounded_whileRecording_discardsTheRecording() {
        var discardCalls = 0
        var uiState by
            mutableStateOf(
                TimeLetterWriteUiState(
                    showVoiceRecorder = true,
                    voiceRecordingState = VoiceRecordingState.Recording(elapsedMillis = 3_000L),
                ),
            )

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AfternoteTheme {
                    TimeLetterWriteScreen(
                        uiState = uiState,
                        titleState = TextFieldState(),
                        onDiscardVoiceRecording = {
                            discardCalls++
                            uiState =
                                uiState.copy(
                                    showVoiceRecorder = false,
                                    voiceRecordingState = VoiceRecordingState.Idle,
                                )
                        },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()

        assertEquals("ON_STOP 은 녹음 중이면 discard 를 정확히 한 번 불러야 한다", 1, discardCalls)
    }

    @Test
    fun appBackgrounded_whileNotRecording_doesNotDiscard() {
        var discardCalls = 0

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AfternoteTheme {
                    TimeLetterWriteScreen(
                        uiState = TimeLetterWriteUiState(showVoiceRecorder = false),
                        titleState = TextFieldState(),
                        onDiscardVoiceRecording = { discardCalls++ },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()

        assertEquals(0, discardCalls)
    }
}
