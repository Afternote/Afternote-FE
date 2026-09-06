package com.afternote.feature.timeletter.presentation

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.testing.FakeFileMetadataRepository
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeVoiceRecorderRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.domain.usecase.ResolveTimeLetterBlocksUseCase
import com.afternote.feature.timeletter.presentation.screen.sender.TimeLetterWriteScreen
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteError
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TimeLetterFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun registerWithoutReceiver_isBlockedAndShownToUser() {
        val repository = FakeTimeLetterRepository()
        val viewModel = viewModel(repository)
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme {
                TimeLetterWriteScreen(
                    uiState = uiState,
                    onRegisterClick = viewModel::register,
                    onErrorShown = viewModel::clearError,
                )
            }
        }

        composeRule.runOnIdle { viewModel.register("보낼 편지", mapOf(0L to "안녕")) }
        composeRule.onNodeWithText("수신자를 선택해주세요.").assertIsDisplayed()

        assertEquals(0, repository.createCalls.size)
    }

    @Test
    fun scheduledSave_failureThenRetry_keepsExactPayload() {
        val repository = FakeTimeLetterRepository()
        val createResults =
            ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("offline")),
                    Result.success(Unit),
                ),
            )
        repository.onCreateTimeLetter = { call ->
            createResults.removeFirst().getOrThrow()
            TimeLetter(
                id = 1L,
                title = call.title,
                sendAt = call.sendAt,
                deliveredAt = null,
                status = call.status,
                blocks = emptyList(),
                receiverIds = call.receiverIds,
            )
        }
        val viewModel = viewModel(repository)
        composeRule.setContent { AfternoteTheme {} }
        composeRule.runOnIdle {
            viewModel.setRecipients(listOf(7L))
            viewModel.setSendAt("2026-09-03")
            viewModel.setSendTime(hour = 14, minute = 35)
            viewModel.addLinkBlock("https://example.test/memory")
            viewModel.register("가을 편지", mapOf(0L to "잊지 않을게"))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.uiState.value.error == TimeLetterWriteError.SaveFailed
        }
        assertFalse(viewModel.uiState.value.registered)

        composeRule.runOnIdle {
            viewModel.clearError()
            viewModel.register("가을 편지", mapOf(0L to "잊지 않을게"))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.registered }

        assertEquals(2, repository.createCalls.size)
        assertEquals(repository.createCalls.first(), repository.createCalls.last())
        val call = repository.createCalls.last()
        assertEquals("가을 편지", call.title)
        assertEquals("2026-09-03T14:35:00", call.sendAt)
        assertEquals(TimeLetterStatus.SCHEDULED, call.status)
        assertEquals(listOf(7L), call.receiverIds)
        assertEquals("잊지 않을게", call.blocks.first().textContent)
        assertEquals("https://example.test/memory", call.blocks.last().url)
    }

    @Test
    fun draftSave_usesDraftStatusWithoutSchedule() {
        val repository = FakeTimeLetterRepository()
        val viewModel = viewModel(repository)
        composeRule.setContent { AfternoteTheme {} }
        composeRule.runOnIdle {
            viewModel.setRecipients(listOf(7L))
            viewModel.saveDraft("임시 편지", mapOf(0L to "이어 쓸 내용"))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.savedAsDraft }

        val call = repository.createCalls.single()
        assertEquals(TimeLetterStatus.DRAFT, call.status)
        assertEquals(null, call.sendAt)
        assertEquals(listOf(7L), call.receiverIds)
    }

    private fun viewModel(repository: FakeTimeLetterRepository): TimeLetterWriteViewModel {
        val resolver =
            ResolveTimeLetterBlocksUseCase(
                FakePhotoUploadRepository(
                    onUpload = { uri, _ ->
                        val name = uri.substringAfterLast('/')
                        Result.success(
                            UploadedFile(
                                fileUrl = "https://cdn.test/$name",
                                fileKey = "timeletters/1/$name",
                            ),
                        )
                    },
                ),
            )
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolver),
            resolveTimeLetterBlocksUseCase = resolver,
            timeLetterRepository = repository,
            userRepository = timeLetterFlowUserRepository(),
            fileMetadataRepository =
                FakeFileMetadataRepository(
                    fileName = "fixture",
                    mimeType = "application/pdf",
                ),
            voiceRecorderRepository = FakeVoiceRecorderRepository,
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to null)),
        )
    }
}

private fun timeLetterFlowUserRepository(): FakeUserRepository =
    FakeUserRepository.strict().apply {
        receiverState.value = listOf(Receiver(7L, "김수신", "가족", "fake-auth-7"))
        onReceiverListFlow = null
        onGetReceivers = null
        onCreateReceiver = null
        onGetMyProfile = null
        onUpdateMyProfile = null
        onDeleteAccount = null
        onGetMyPushSettings = null
        onUpdateMyPushSettings = null
        onGetConnectedAccounts = null
    }
