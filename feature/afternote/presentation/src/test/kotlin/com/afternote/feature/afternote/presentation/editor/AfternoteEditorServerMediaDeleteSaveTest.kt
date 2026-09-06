package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.NoopAuthorErrorReporter
import com.afternote.feature.afternote.presentation.afternoteAuthorUserRepository
import com.afternote.feature.afternote.presentation.afternoteEditorSavedStateHandle
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** 수정 상세의 서버 미디어 삭제가 저장·재진입까지 유지되는지 검증한다(#1597). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorServerMediaDeleteSaveTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `서버 사진과 영상을 지운 전체 폼을 저장하면 payload와 새 수정 진입에서도 빈다`() =
        runTest(dispatcher) {
            val repository = FakeAfternoteRepository(initialDetails = mapOf(AFTERNOTE_ID to serverMemorialDetail()))
            val first = viewModel(repository)
            collectState(first)
            applyLoadedPrefill(first)
            assertServerMediaAndSongs(first.currentForm())

            first.removeMemorialPhoto()
            first.removeMemorialVideo()
            val deletedForm = first.currentForm()

            assertDeletedMediaAndSongs(deletedForm)
            first.saveAfternote(
                payload = validMemorialPayload(),
                selectedReceiverIds = emptyList(),
                memorialMedia = deletedForm.fullMemorialMediaForSave(),
            )
            advanceUntilIdle()

            val updatePayload = repository.updateCalls.single().second
            val memorial = requireNotNull(updatePayload.memorial)
            assertNull(memorial.memorialPhotoUrl)
            assertNull(memorial.memorialVideo)
            assertEquals(
                listOf(MemorialSongPayload(title = "배경음악", artist = "작곡가", coverUrl = "https://cdn.test/cover.jpg")),
                memorial.songs,
            )
            assertEquals(AFTERNOTE_ID, first.uiState.value.savedId)

            val updatedMedia = repository.details.getValue(AFTERNOTE_ID).memorialMedia()
            assertNull(updatedMedia.photoUrl)
            assertNull(updatedMedia.videoUrl)
            assertNull(updatedMedia.thumbnailUrl)

            val reentered = viewModel(repository)
            collectState(reentered)
            applyLoadedPrefill(reentered)

            assertDeletedMediaAndSongs(reentered.currentForm())
        }

    @Test
    fun `저장이 실패해도 삭제한 폼을 유지해 같은 null 미디어로 재시도한다`() =
        runTest(dispatcher) {
            val updateResults =
                ArrayDeque(
                    listOf(
                        Result.failure(IOException("offline")),
                        Result.success(AFTERNOTE_ID),
                    ),
                )
            val repository =
                FakeAfternoteRepository(initialDetails = mapOf(AFTERNOTE_ID to serverMemorialDetail())).apply {
                    onUpdate = { _, _ -> updateResults.removeFirst() }
                }
            val viewModel = viewModel(repository)
            collectState(viewModel)
            applyLoadedPrefill(viewModel)
            viewModel.removeMemorialPhoto()
            viewModel.removeMemorialVideo()

            viewModel.saveCurrentMemorialForm()
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.savedId)
            assertDeletedMediaAndSongs(viewModel.currentForm())

            viewModel.saveCurrentMemorialForm()
            advanceUntilIdle()

            assertEquals(AFTERNOTE_ID, viewModel.uiState.value.savedId)
            assertEquals(2, repository.updateCalls.size)
            repository.updateCalls.forEach { (_, payload) ->
                val memorial = requireNotNull(payload.memorial)
                assertNull(memorial.memorialPhotoUrl)
                assertNull(memorial.memorialVideo)
                assertTrue(memorial.songs.isNotEmpty())
            }
        }

    private fun TestScope.collectState(viewModel: AfternoteEditorViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
    }

    private fun TestScope.applyLoadedPrefill(viewModel: AfternoteEditorViewModel) {
        advanceUntilIdle()
        val prefill = requireNotNull(viewModel.uiState.value.pendingPrefill)
        viewModel.applyPrefill(prefill)
        viewModel.onPrefillConsumed()
        advanceUntilIdle()
    }

    private fun AfternoteEditorViewModel.saveCurrentMemorialForm() {
        val form = currentForm()
        saveAfternote(
            payload = validMemorialPayload(),
            selectedReceiverIds = emptyList(),
            memorialMedia = form.fullMemorialMediaForSave(),
        )
    }

    private fun EditorFormState.fullMemorialMediaForSave(): SaveAfternoteMemorialMedia =
        SaveAfternoteMemorialMedia(
            memorialVideo = memorialVideo ?: EditableMemorialVideo.empty(),
            memorialPhotoUrl = memorialPhotoUrl,
            pickedMemorialPhotoUri = pickedMemorialPhotoUri,
        )

    private fun assertServerMediaAndSongs(form: EditorFormState) {
        assertEquals("https://cdn.test/portrait.jpg", form.memorialPhotoUrl)
        assertEquals("https://cdn.test/farewell.mp4", form.displayedMemorialVideo?.url)
        assertEquals("https://cdn.test/thumbnail.jpg", form.displayedMemorialVideo?.thumbnailUrl)
        assertEquals(listOf("배경음악"), form.memorialPlaylistSongs.map { it.title })
    }

    private fun assertDeletedMediaAndSongs(form: EditorFormState) {
        assertNull(form.pickedMemorialPhotoUri)
        assertNull(form.memorialPhotoUrl)
        assertNull(form.displayMemorialPhotoUri())
        assertNull(form.displayedMemorialVideo?.url)
        assertNull(form.displayedMemorialVideo?.thumbnailUrl)
        assertEquals(listOf("배경음악"), form.memorialPlaylistSongs.map { it.title })
    }

    private fun viewModel(repository: FakeAfternoteRepository): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle =
                afternoteEditorSavedStateHandle(
                    initialType = AfternoteType.MEMORIAL,
                    itemId = AFTERNOTE_ID,
                ),
            userRepository = afternoteAuthorUserRepository(),
            afternoteRepository = repository,
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { input, _ ->
                        when (input) {
                            MediaInput.None -> Result.success(null)
                            is MediaInput.Remote -> Result.success(input.url)
                            is MediaInput.Local -> error("삭제 저장에서 로컬 미디어를 업로드하면 안 됩니다")
                        }
                    },
                ),
            errorReporter = NoopAuthorErrorReporter,
        )

    private fun validMemorialPayload(): RegisterAfternotePayload =
        RegisterAfternotePayload(
            serviceName = "추억 노트",
            date = "2026.08.30",
        )

    private fun serverMemorialDetail(): Detail =
        Detail(
            id = AFTERNOTE_ID,
            serviceName = "추억 노트",
            timestamps = DetailTimestamps(updatedAt = "2026.08.30"),
            receivers = emptyList(),
            leaveMessageBlocks = emptyList(),
            content =
                DetailContent.Memorial(
                    memorial =
                        MemorialDetail(
                            songs =
                                listOf(
                                    DetailSong(
                                        title = "배경음악",
                                        artist = "작곡가",
                                        coverUrl = "https://cdn.test/cover.jpg",
                                    ),
                                ),
                            media =
                                MemorialMedia(
                                    photoUrl = "https://cdn.test/portrait.jpg",
                                    videoUrl = "https://cdn.test/farewell.mp4",
                                    thumbnailUrl = "https://cdn.test/thumbnail.jpg",
                                ),
                        ),
                ),
        )

    private fun Detail.memorialMedia(): MemorialMedia = (content as DetailContent.Memorial).memorial.media

    private companion object {
        const val AFTERNOTE_ID = 1597L
    }
}
