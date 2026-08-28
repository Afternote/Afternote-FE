package com.afternote.feature.afternote.presentation.author

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class AfternoteAuthorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingReceiver_blocksSaveAndExposesValidationSemantics() {
        val repository = FakeAfternoteRepository.strict()
        val viewModel = viewModel(repository, afternoteEditorSavedStateHandle(AfternoteType.SOCIAL_NETWORK))
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme {
                (uiState.error as? AfternoteEditorError.Validation)?.let {
                    Text(stringResource(it.reason.messageResId))
                }
            }
        }

        composeRule.runOnIdle {
            viewModel.setType(AfternoteType.SOCIAL_NETWORK)
            viewModel.saveAfternote(
                payload = validSocialPayload(),
                selectedReceiverIds = emptyList(),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
        }

        composeRule.onNodeWithText("수신자를 한 명 이상 선택해 주세요.").assertIsDisplayed()
        assertEquals(0, repository.socialPayloads.size)
        assertEquals(
            AfternoteEditorError.Validation(AfternoteValidationError.RECEIVERS_REQUIRED),
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun createFailureThenRetry_keepsExactPayloadAndEmitsSingleSuccess() {
        val createSocialResults =
            ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("offline")),
                    Result.success(42L),
                ),
            )
        val repository =
            FakeAfternoteRepository.strict().apply {
                onCreateSocial = { createSocialResults.removeFirst() }
            }
        val viewModel = viewModel(repository, afternoteEditorSavedStateHandle(AfternoteType.SOCIAL_NETWORK))
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme { Text(uiState.savedId?.toString().orEmpty()) }
        }
        val payload = validSocialPayload()

        composeRule.runOnIdle {
            viewModel.setType(AfternoteType.SOCIAL_NETWORK)
            viewModel.saveAfternote(payload, listOf(7L), SaveAfternoteMemorialMedia())
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.error != null }
        assertNull(viewModel.uiState.value.savedId)

        composeRule.runOnIdle {
            viewModel.setType(AfternoteType.SOCIAL_NETWORK)
            viewModel.saveAfternote(payload, listOf(7L), SaveAfternoteMemorialMedia())
        }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.uiState.value.savedId == 42L }

        assertEquals(2, repository.socialPayloads.size)
        assertEquals(repository.socialPayloads.first(), repository.socialPayloads.last())
        val sent = repository.socialPayloads.last()
        assertEquals("Instagram", sent.title)
        assertEquals(listOf(7L), sent.receiverIds)
        assertEquals("author@example.test", sent.credentials?.id)
        assertEquals("계정 삭제", sent.processingMethods.single())
    }

    @Test
    fun savedState_recreatesTypeReceiverAndProcessingForm() {
        val handle = afternoteEditorSavedStateHandle(AfternoteType.GALLERY_AND_FILES)
        val first = viewModel(FakeAfternoteRepository.strict(), handle)
        composeRule.setContent { AfternoteTheme {} }
        composeRule.runOnIdle {
            first.setType(AfternoteType.GALLERY_AND_FILES)
            first.setService("Google Photos")
            first.addReceiverIfAbsent(7L, "김수신", "가족")
            first.addProcessingMethod("전체 파일 전달")
        }

        val restored = viewModel(FakeAfternoteRepository.strict(), handle).currentForm()

        assertEquals(AfternoteType.GALLERY_AND_FILES, restored.selectedType)
        assertEquals("Google Photos", restored.selectedService)
        assertEquals(7L, restored.afternoteEditReceivers.single().id)
        assertEquals("전체 파일 전달", restored.processingMethods.single().text)
    }

    private fun validSocialPayload() =
        RegisterAfternotePayload(
            serviceName = "Instagram",
            date = "2026.08.22",
            accountId = "author@example.test",
            password = "password-1234",
            messageBlocks = listOf(EditorMessageTextBlock("마지막 말", "고마웠어")),
            processingMethods = listOf("계정 삭제"),
        )

    private fun viewModel(
        repository: FakeAfternoteRepository,
        savedStateHandle: SavedStateHandle,
    ): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle = savedStateHandle,
            userRepository = afternoteAuthorUserRepository(),
            afternoteRepository = repository,
            memorialThumbnailUploadRepository = MemorialThumbnailUploadRepository { Result.success("https://cdn.test/thumb.jpg") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    memorialMediaUploadRepository =
                        MemorialMediaUploadRepository { input, kind ->
                            Result.success(
                                when (input) {
                                    MediaInput.None -> {
                                        null
                                    }

                                    is MediaInput.Local -> {
                                        when (kind) {
                                            MediaKind.VIDEO -> "https://cdn.test/video.mp4"
                                            MediaKind.PHOTO -> "https://cdn.test/photo.jpg"
                                        }
                                    }

                                    is MediaInput.Remote -> {
                                        input.url
                                    }
                                },
                            )
                        },
                ),
            errorReporter = NoopAuthorErrorReporter,
        )
}
