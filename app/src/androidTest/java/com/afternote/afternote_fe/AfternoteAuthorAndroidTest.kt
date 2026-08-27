package com.afternote.afternote_fe

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.FakeErrorReporter
import com.afternote.afternote_fe.test.afternoteEditorSavedStateHandle
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AfternoteAuthorAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun missingReceiver_blocksSaveAndExposesValidationSemantics() {
        val repository = FakeAfternoteRepository()
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
        assertEquals(0, repository.createSocialPayloads.size)
        assertEquals(
            AfternoteEditorError.Validation(AfternoteValidationError.RECEIVERS_REQUIRED),
            viewModel.uiState.value.error,
        )
    }

    @Test
    fun createFailureThenRetry_keepsExactPayloadAndEmitsSingleSuccess() {
        val repository = FakeAfternoteRepository()
        repository.createSocialResults.addLast(Result.failure(IllegalStateException("offline")))
        repository.createSocialResults.addLast(Result.success(42L))
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

        assertEquals(2, repository.createSocialPayloads.size)
        assertEquals(repository.createSocialPayloads.first(), repository.createSocialPayloads.last())
        val sent = repository.createSocialPayloads.last()
        assertEquals("Instagram", sent.title)
        assertEquals(listOf(7L), sent.receiverIds)
        assertEquals("author@example.test", sent.credentials?.id)
        assertEquals("계정 삭제", sent.processingMethods.single())
    }

    @Test
    fun savedState_recreatesTypeReceiverAndProcessingForm() {
        val handle = afternoteEditorSavedStateHandle(AfternoteType.GALLERY_AND_FILES)
        val first = viewModel(FakeAfternoteRepository(), handle)
        composeRule.setContent { AfternoteTheme {} }
        composeRule.runOnIdle {
            first.setType(AfternoteType.GALLERY_AND_FILES)
            first.setService("Google Photos")
            first.addReceiverIfAbsent("7", "김수신", "가족")
            first.addProcessingMethod("전체 파일 전달")
        }

        val restored = viewModel(FakeAfternoteRepository(), handle).currentForm()

        assertEquals(AfternoteType.GALLERY_AND_FILES, restored.selectedType)
        assertEquals("Google Photos", restored.selectedService)
        assertEquals("7", restored.afternoteEditReceivers.single().id)
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
            userRepository = appTestUserRepository(),
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
            errorReporter = FakeErrorReporter(),
        )
}

private class FakeAfternoteRepository : AfternoteRepository {
    val createSocialPayloads = mutableListOf<CreateAccountPayload>()
    val createSocialResults = ArrayDeque<Result<Long>>()

    override fun getPagedAfternotes(type: AfternoteType?): Flow<PagingData<ListItem>> = flowOf(PagingData.empty())

    override suspend fun getDetail(id: Long): Result<Detail> = Result.failure(NoSuchElementException())

    override suspend fun createSocial(payload: CreateAccountPayload): Result<Long> {
        createSocialPayloads += payload
        return createSocialResults.removeFirstOrNull() ?: Result.success(1L)
    }

    override suspend fun createBusiness(payload: CreateAccountPayload): Result<Long> = error("unexpected createBusiness")

    override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> = error("unexpected createGallery")

    override suspend fun createMemorial(payload: CreateMemorialPayload): Result<Long> = error("unexpected createMemorial")

    override suspend fun update(
        id: Long,
        payload: AfternoteUpdatePayload,
    ): Result<Long> = error("unexpected update")

    override suspend fun delete(id: Long): Result<Unit> = error("unexpected delete")
}
