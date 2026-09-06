package com.afternote.feature.afternote.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.MemorialVideoAttachment
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
class AfternoteAuthorImplementedCoverageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun gallerySave_routesTrimmedGalleryPayloadToOnlyGalleryEndpoint() {
        val repository = implementedCoverageRepository(AfternoteType.GALLERY_AND_FILES)
        val viewModel =
            implementedCoverageViewModel(
                repository = repository,
                savedStateHandle = afternoteEditorSavedStateHandle(AfternoteType.GALLERY_AND_FILES),
            )
        collectSaveState(viewModel)

        composeRule.runOnIdle {
            viewModel.setType(AfternoteType.GALLERY_AND_FILES)
            viewModel.saveAfternote(
                payload =
                    RegisterAfternotePayload(
                        serviceName = "Google Drive",
                        date = "2026.08.22",
                        messageBlocks =
                            listOf(
                                EditorMessageTextBlock(" 사진 ", " 여행 사진을 보관해 줘 "),
                                EditorMessageTextBlock("", ""),
                            ),
                        processingMethods = listOf("가족에게 폴더 전달"),
                    ),
                selectedReceiverIds = listOf(7L, 8L),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.galleryPayloads.size == 1 && viewModel.uiState.value.savedId == 501L
        }
        assertEquals(
            CreateGalleryPayload(
                title = "Google Drive",
                processingMethods = listOf("가족에게 폴더 전달"),
                leaveMessageBlocks = listOf(LeaveMessageBlock("사진", "여행 사진을 보관해 줘")),
                receiverIds = listOf(7L, 8L),
            ),
            repository.galleryPayloads.single(),
        )
        assertEquals(0, repository.socialPayloads.size + repository.businessPayloads.size)
        assertEquals(0, repository.memorialPayloads.size)
    }

    @Test
    fun businessSave_routesCredentialsAndActionsToBusinessEndpoint() {
        val repository = implementedCoverageRepository(AfternoteType.BUSINESS)
        val viewModel =
            implementedCoverageViewModel(
                repository = repository,
                savedStateHandle = afternoteEditorSavedStateHandle(AfternoteType.BUSINESS),
            )
        collectSaveState(viewModel)

        composeRule.runOnIdle {
            viewModel.setType(AfternoteType.BUSINESS)
            viewModel.saveAfternote(
                payload =
                    RegisterAfternotePayload(
                        serviceName = "회사 그룹웨어",
                        date = "2026.08.22",
                        accountId = "employee@example.test",
                        password = "business-password",
                        messageBlocks = listOf(EditorMessageTextBlock("인수인계", "팀장에게 전달해 줘")),
                        processingMethods = listOf("계정 인계"),
                    ),
                selectedReceiverIds = listOf(8L),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.businessPayloads.size == 1 && viewModel.uiState.value.savedId == 503L
        }
        assertEquals(
            CreateAccountPayload(
                title = "회사 그룹웨어",
                processingMethods = listOf("계정 인계"),
                leaveMessageBlocks = listOf(LeaveMessageBlock("인수인계", "팀장에게 전달해 줘")),
                credentials =
                    AfternoteAccountCredentials(
                        id = "employee@example.test",
                        password = "business-password",
                    ),
                receiverIds = listOf(8L),
            ),
            repository.businessPayloads.single(),
        )
        assertEquals(0, repository.socialPayloads.size)
        assertEquals(0, repository.galleryPayloads.size)
        assertEquals(0, repository.memorialPayloads.size)
    }

    @Test
    fun memorialSave_resolvesLocalMediaAndForwardsExactPlaylistPayload() {
        val repository = implementedCoverageRepository(AfternoteType.MEMORIAL)
        val videoInputs = mutableListOf<MediaInput>()
        val photoInputs = mutableListOf<MediaInput>()
        val viewModel =
            implementedCoverageViewModel(
                repository = repository,
                savedStateHandle = afternoteEditorSavedStateHandle(AfternoteType.MEMORIAL),
                videoInputs = videoInputs,
                photoInputs = photoInputs,
            )
        collectSaveState(viewModel)
        val songs =
            listOf(
                Song("91", "첫 번째 노래", "가수 A", "https://cdn.test/cover-a.jpg"),
                Song("local-song", "두 번째 노래", "가수 B"),
            )

        composeRule.runOnIdle {
            viewModel.setType(AfternoteType.MEMORIAL)
            viewModel.addMemorialPlaylistSongs(songs)
            viewModel.saveAfternote(
                payload =
                    RegisterAfternotePayload(
                        serviceName = "추억 노트",
                        date = "2026.08.22",
                    ),
                selectedReceiverIds = listOf(7L),
                memorialMedia =
                    SaveAfternoteMemorialMedia(
                        memorialVideo =
                            EditableMemorialVideo
                                .empty()
                                .withSelection("content://videos/farewell")
                                .withSelectionThumbnail("https://cdn.test/thumbnail.jpg"),
                        memorialPhotoUrl = "https://cdn.test/old-photo.jpg",
                        pickedMemorialPhotoUri = "content://photos/new-portrait",
                    ),
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.memorialPayloads.size == 1 && viewModel.uiState.value.savedId == 502L
        }
        assertEquals(
            listOf(MediaInput.Local("content://videos/farewell")),
            videoInputs,
        )
        assertEquals(
            listOf(MediaInput.Local("content://photos/new-portrait")),
            photoInputs,
        )
        assertEquals(
            CreateMemorialPayload(
                title = "추억 노트",
                memorial =
                    MemorialWritePayload(
                        memorialPhotoUrl = "https://cdn.test/uploaded-photo.jpg",
                        songs =
                            listOf(
                                MemorialSongPayload(
                                    title = "첫 번째 노래",
                                    artist = "가수 A",
                                    coverUrl = "https://cdn.test/cover-a.jpg",
                                ),
                                MemorialSongPayload(
                                    title = "두 번째 노래",
                                    artist = "가수 B",
                                    coverUrl = null,
                                ),
                            ),
                        memorialVideo =
                            MemorialVideoPayload(
                                videoUrl = "https://cdn.test/uploaded-video.mp4",
                                thumbnailUrl = "https://cdn.test/thumbnail.jpg",
                            ),
                    ),
                receiverIds = listOf(7L),
            ),
            repository.memorialPayloads.single(),
        )
        assertEquals(0, repository.socialPayloads.size + repository.businessPayloads.size)
        assertEquals(0, repository.galleryPayloads.size)
    }

    @Test
    fun memorialMediaAndPlaylist_recreateFromSavedStateWithoutLosingSelection() {
        val handle = afternoteEditorSavedStateHandle(AfternoteType.MEMORIAL)
        val first = implementedCoverageViewModel(implementedCoverageRepository(), handle)
        composeRule.setContent { AfternoteTheme {} }

        composeRule.runOnIdle {
            first.setType(AfternoteType.MEMORIAL)
            first.setMemorialVideo("content://videos/farewell")
            first.setMemorialPhoto("content://photos/portrait")
            first.setMemorialThumbnail("https://cdn.test/thumbnail.jpg")
            first.addMemorialPlaylistSongs(
                listOf(Song("91", "첫 번째 노래", "가수 A", "https://cdn.test/cover.jpg")),
            )
        }

        val restored = implementedCoverageViewModel(implementedCoverageRepository(), handle).currentForm()
        assertEquals(AfternoteType.MEMORIAL, restored.selectedType)
        assertEquals(
            MemorialVideoAttachment(
                url = "content://videos/farewell",
                thumbnailUrl = "https://cdn.test/thumbnail.jpg",
            ),
            restored.displayedMemorialVideo,
        )
        assertEquals("content://photos/portrait", restored.pickedMemorialPhotoUri)
        assertEquals(
            listOf(Song("91", "첫 번째 노래", "가수 A", "https://cdn.test/cover.jpg")),
            restored.memorialPlaylistSongs,
        )
    }

    private fun collectSaveState(viewModel: AfternoteEditorViewModel) {
        composeRule.setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AfternoteTheme { Text(uiState.savedId?.toString().orEmpty()) }
        }
    }
}

private fun implementedCoverageRepository(allowedCreateType: AfternoteType? = null): FakeAfternoteRepository =
    FakeAfternoteRepository.strict().apply {
        when (allowedCreateType) {
            AfternoteType.SOCIAL_NETWORK -> onCreateSocial = { Result.success(500L) }
            AfternoteType.BUSINESS -> onCreateBusiness = { Result.success(503L) }
            AfternoteType.GALLERY_AND_FILES -> onCreateGallery = { Result.success(501L) }
            AfternoteType.MEMORIAL -> onCreateMemorial = { Result.success(502L) }
            AfternoteType.ESTATE, null -> Unit
        }
    }

private fun implementedCoverageViewModel(
    repository: FakeAfternoteRepository,
    savedStateHandle: SavedStateHandle,
    videoInputs: MutableList<MediaInput> = mutableListOf(),
    photoInputs: MutableList<MediaInput> = mutableListOf(),
): AfternoteEditorViewModel =
    AfternoteEditorViewModel(
        savedStateHandle = savedStateHandle,
        userRepository = afternoteAuthorUserRepository(),
        afternoteRepository = repository,
        memorialThumbnailUploadRepository =
            MemorialThumbnailUploadRepository {
                Result.success("https://cdn.test/thumbnail.jpg")
            },
        resolveMemorialMediaForSave =
            ResolveMemorialMediaForSaveUseCase(
                memorialMediaUploadRepository =
                    MemorialMediaUploadRepository { input, kind ->
                        when (kind) {
                            MediaKind.VIDEO -> videoInputs += input
                            MediaKind.PHOTO -> photoInputs += input
                        }
                        Result.success(
                            when (input) {
                                MediaInput.None -> {
                                    null
                                }

                                is MediaInput.Local -> {
                                    when (kind) {
                                        MediaKind.VIDEO -> "https://cdn.test/uploaded-video.mp4"
                                        MediaKind.PHOTO -> "https://cdn.test/uploaded-photo.jpg"
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
