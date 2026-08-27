package com.afternote.afternote_fe

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.author.editor.SaveAfternoteMemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AfternoteAuthorImplementedCoverageAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun gallerySave_routesTrimmedGalleryPayloadToOnlyGalleryEndpoint() {
        val repository = ImplementedCoverageAfternoteRepository()
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
        assertEquals(0, repository.accountCreateCalls)
        assertEquals(0, repository.memorialPayloads.size)
    }

    @Test
    fun businessSave_routesCredentialsAndActionsToBusinessEndpoint() {
        val repository = ImplementedCoverageAfternoteRepository()
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
        val repository = ImplementedCoverageAfternoteRepository()
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
                        memorialVideoUrl = "content://videos/farewell",
                        memorialThumbnailUrl = "https://cdn.test/thumbnail.jpg",
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
        assertEquals(0, repository.accountCreateCalls)
        assertEquals(0, repository.galleryPayloads.size)
    }

    @Test
    fun memorialMediaAndPlaylist_recreateFromSavedStateWithoutLosingSelection() {
        val handle = afternoteEditorSavedStateHandle(AfternoteType.MEMORIAL)
        val first = implementedCoverageViewModel(ImplementedCoverageAfternoteRepository(), handle)
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

        val restored = implementedCoverageViewModel(ImplementedCoverageAfternoteRepository(), handle).currentForm()
        assertEquals(AfternoteType.MEMORIAL, restored.selectedType)
        assertEquals("content://videos/farewell", restored.memorialVideoUrl)
        assertEquals("content://photos/portrait", restored.pickedMemorialPhotoUri)
        assertEquals("https://cdn.test/thumbnail.jpg", restored.memorialThumbnailUrl)
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

private class ImplementedCoverageAfternoteRepository : AfternoteRepository {
    val socialPayloads = mutableListOf<CreateAccountPayload>()
    val businessPayloads = mutableListOf<CreateAccountPayload>()
    val galleryPayloads = mutableListOf<CreateGalleryPayload>()
    val memorialPayloads = mutableListOf<CreateMemorialPayload>()
    var accountCreateCalls = 0

    override fun getPagedAfternotes(type: AfternoteType?): Flow<PagingData<ListItem>> = flowOf(PagingData.empty())

    override suspend fun getDetail(id: Long): Result<Detail> = Result.failure(NoSuchElementException())

    override suspend fun createSocial(payload: CreateAccountPayload): Result<Long> {
        accountCreateCalls += 1
        socialPayloads += payload
        return Result.success(500L)
    }

    override suspend fun createBusiness(payload: CreateAccountPayload): Result<Long> {
        accountCreateCalls += 1
        businessPayloads += payload
        return Result.success(503L)
    }

    override suspend fun createGallery(payload: CreateGalleryPayload): Result<Long> {
        galleryPayloads += payload
        return Result.success(501L)
    }

    override suspend fun createMemorial(payload: CreateMemorialPayload): Result<Long> {
        memorialPayloads += payload
        return Result.success(502L)
    }

    override suspend fun update(
        id: Long,
        payload: AfternoteUpdatePayload,
    ): Result<Long> = error("unexpected update")

    override suspend fun delete(id: Long): Result<Unit> = error("unexpected delete")
}

private fun implementedCoverageViewModel(
    repository: ImplementedCoverageAfternoteRepository,
    savedStateHandle: SavedStateHandle,
    videoInputs: MutableList<MediaInput> = mutableListOf(),
    photoInputs: MutableList<MediaInput> = mutableListOf(),
): AfternoteEditorViewModel =
    AfternoteEditorViewModel(
        savedStateHandle = savedStateHandle,
        userRepository = appTestUserRepository(),
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
        errorReporter = FakeErrorReporter(),
    )
