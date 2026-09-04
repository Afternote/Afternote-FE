package com.afternote.afternote_fe

import android.Manifest
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.afternote_fe.test.testReceiver
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.data.testing.createVoiceRecorderRepositoryForTesting
import com.afternote.feature.timeletter.domain.testing.FakeFileMetadataRepository
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.domain.usecase.ResolveTimeLetterBlocksUseCase
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.VoiceRecordingState
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 녹음 중 화면 이탈(`ON_STOP`)이 실제 Activity 생명주기 위에서, 실제
 * `TimeLetterWriteScreen → TimeLetterWriteViewModel.discardVoiceRecording() →
 * VoiceRecorderRepository.discard() → 파일 삭제` 결선을 실행하는지 검증한다. 이전 버전은
 * synthetic uiState 만 바꾸는 로컬 람다로 짜여 있어 이 결선을 한 번도 타지 않았다 (#440 리뷰).
 * `VoiceRecorderRepository` 는 fake 가 아니라 `createVoiceRecorderRepositoryForTesting` 로 얻은
 * 실제 구현이라, discard 가 실제 `MediaRecorder`/`filesDir` 자원까지 정리하는지도 함께 본다.
 */
@RunWith(AndroidJUnit4::class)
class VoiceRecordingLifecycleAndroidTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audioDirectory = File(context.filesDir, "timeletter_audio")

    @Before
    fun grantMicrophonePermission() {
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
    }

    @Test
    fun appBackgrounded_whileRecording_discardsTheRecordingAndDeletesTheFile() {
        val viewModel = timeLetterWriteViewModel()
        renderWriteScreen(viewModel)

        composeRule.runOnIdle {
            viewModel.openVoiceRecorder()
            viewModel.startVoiceRecording()
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.voiceRecordingState is VoiceRecordingState.Recording
        }

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.voiceRecordingState == VoiceRecordingState.Idle
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { audioDirectory.listFiles().isNullOrEmpty() }
        assertTrue(
            "ON_STOP 이 discardVoiceRecording() -> repository.discard() 를 실제로 태워 파일을 지워야 한다",
            audioDirectory.listFiles().isNullOrEmpty(),
        )
    }

    @Test
    fun appBackgrounded_immediatelyAfterStarting_discardsTheLateArrivingRecording() {
        // start() 완료 전에 화면을 떠나는 경주 상황이다 (#440 리뷰 P1) — Starting 을 동기
        // 반영하고 세대(generation) 로 뒤늦은 성공을 걸러내는지를 최종 파일 상태로 확인한다.
        val viewModel = timeLetterWriteViewModel()
        renderWriteScreen(viewModel)

        composeRule.runOnIdle {
            viewModel.openVoiceRecorder()
            viewModel.startVoiceRecording()
        }
        // runOnIdle 은 액션 실행 "전" idle 만 보장한다 — 이 recomposition 이 실제로 반영돼
        // LifecycleEventEffect 의 클로저가 Starting 을 보게 된 뒤에 백그라운드로 보내야,
        // ON_STOP 조건이 방금 반영된 상태가 아니라 그 이전 stale Idle 을 보고 건너뛰지 않는다.
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.voiceRecordingState == VoiceRecordingState.Idle
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) { audioDirectory.listFiles().isNullOrEmpty() }
        assertTrue(
            "Starting 중 이탈해도 뒤늦게 성공한 start() 결과가 남기는 녹음 파일이 정리돼야 한다",
            audioDirectory.listFiles().isNullOrEmpty(),
        )
    }

    @Test
    fun appBackgrounded_afterRecordingFinished_doesNotDiscardTheRecordedAudio() {
        val viewModel = timeLetterWriteViewModel()
        renderWriteScreen(viewModel)

        composeRule.runOnIdle {
            viewModel.openVoiceRecorder()
            viewModel.startVoiceRecording()
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.voiceRecordingState is VoiceRecordingState.Recording
        }
        composeRule.runOnIdle { viewModel.stopVoiceRecording() }
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value.voiceRecordingState is VoiceRecordingState.Recorded
        }

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.waitForIdle()

        assertTrue(
            "녹음이 끝난 뒤(Recorded)에는 ON_STOP 이 결과를 지우면 안 된다",
            viewModel.uiState.value.voiceRecordingState is VoiceRecordingState.Recorded,
        )
        assertTrue(viewModel.uiState.value.showVoiceRecorder)

        viewModel.discardVoiceRecording()
    }

    private fun renderWriteScreen(viewModel: TimeLetterWriteViewModel) {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AfternoteTheme {
                    TimeLetterWriteScreen(
                        uiState = uiState,
                        titleState = TextFieldState(),
                        onOpenVoiceRecorder = viewModel::openVoiceRecorder,
                        onStartVoiceRecording = viewModel::startVoiceRecording,
                        onStopVoiceRecording = viewModel::stopVoiceRecording,
                        onDiscardVoiceRecording = viewModel::discardVoiceRecording,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun timeLetterWriteViewModel(): TimeLetterWriteViewModel {
        val repository = FakeTimeLetterRepository.strict()
        val userRepository = appTestUserRepository(receivers = listOf(testReceiver()))
        val resolver = ResolveTimeLetterBlocksUseCase(FakePhotoUploadRepository.strict())
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolver),
            resolveTimeLetterBlocksUseCase = resolver,
            timeLetterRepository = repository,
            userRepository = userRepository,
            fileMetadataRepository = FakeFileMetadataRepository.strict(),
            voiceRecorderRepository = createVoiceRecorderRepositoryForTesting(context, Dispatchers.IO),
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to null)),
        )
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
