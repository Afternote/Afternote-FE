package com.afternote.feature.afternote.presentation.author.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

/**
 * 추억 노트 미디어 슬롯의 삭제 경로 (#1114, #1597) —
 * `setMemorialPhoto(null)`/`setMemorialVideo(null)`.
 *
 * 삭제는 새 세터가 아니라 기존 nullable 세터의 null 인자로 표현된다. 로컬 첨부가 서버 원본을
 * 덮고 있으면 첫 삭제는 로컬 층만 비워 서버 원본으로 돌아간다. 서버 층만 남은 다음 삭제는
 * 원본도 비우며, 그 상태는 SavedState 스냅샷에 실린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorMemorialMediaRemoveTest {
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
    fun `신규 영상을 삭제하면 파생 썸네일도 함께 비운다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.setMemorialVideo("content://videos/farewell")
        viewModel.setMemorialThumbnail("https://cdn.test/thumbnail.jpg")

        viewModel.setMemorialVideo(null)

        assertNull(viewModel.currentForm().memorialVideoUrl)
        assertNull(viewModel.currentForm().memorialThumbnailUrl)
    }

    @Test
    fun `사진은 새 로컬 픽을 먼저 지운 뒤 서버 원본까지 지운다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.applyPrefill(memorialPrefill(photoUrl = "https://cdn.test/portrait.jpg"))
        viewModel.setMemorialPhoto("content://photos/replacement")
        assertEquals("content://photos/replacement", viewModel.currentForm().displayMemorialPhotoUri())

        viewModel.setMemorialPhoto(null)

        assertNull(viewModel.currentForm().pickedMemorialPhotoUri)
        assertEquals("https://cdn.test/portrait.jpg", viewModel.currentForm().memorialPhotoUrl)
        assertEquals("https://cdn.test/portrait.jpg", viewModel.currentForm().displayMemorialPhotoUri())

        viewModel.setMemorialPhoto(null)

        assertNull(viewModel.currentForm().memorialPhotoUrl)
        assertNull(viewModel.currentForm().displayMemorialPhotoUri())
    }

    @Test
    fun `영상은 새 로컬 픽을 먼저 지운 뒤 서버 원본과 썸네일까지 지운다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.applyPrefill(
            memorialPrefill(
                videoUrl = "https://cdn.test/farewell.mp4",
                thumbnailUrl = "https://cdn.test/server-thumbnail.jpg",
            ),
        )
        viewModel.setMemorialVideo("content://videos/replacement")
        viewModel.setMemorialThumbnail("https://cdn.test/local-thumbnail.jpg")

        viewModel.setMemorialVideo(null)

        assertEquals("https://cdn.test/farewell.mp4", viewModel.currentForm().memorialVideoUrl)
        assertEquals("https://cdn.test/server-thumbnail.jpg", viewModel.currentForm().memorialThumbnailUrl)

        viewModel.setMemorialVideo(null)

        assertNull(viewModel.currentForm().memorialVideoUrl)
        assertNull(viewModel.currentForm().memorialThumbnailUrl)
    }

    @Test
    fun `서버 미디어만 있어도 사진과 영상을 삭제할 수 있다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.applyPrefill(
            memorialPrefill(
                photoUrl = "https://cdn.test/portrait.jpg",
                videoUrl = "https://cdn.test/farewell.mp4",
                thumbnailUrl = "https://cdn.test/thumbnail.jpg",
            ),
        )

        viewModel.setMemorialPhoto(null)
        viewModel.setMemorialVideo(null)

        assertNull(viewModel.currentForm().pickedMemorialPhotoUri)
        assertNull(viewModel.currentForm().memorialPhotoUrl)
        assertNull(viewModel.currentForm().displayMemorialPhotoUri())
        assertNull(viewModel.currentForm().memorialVideoUrl)
        assertNull(viewModel.currentForm().memorialThumbnailUrl)
    }

    @Test
    fun `삭제한 서버 미디어 스냅샷은 상세 재조회 없는 복원에서도 비어 있다`() {
        val handle = memorialSavedStateHandle()
        val first = viewModel(handle)
        first.applyPrefill(
            memorialPrefill(
                photoUrl = "https://cdn.test/portrait.jpg",
                videoUrl = "https://cdn.test/farewell.mp4",
                thumbnailUrl = "https://cdn.test/thumbnail.jpg",
                playlistSongs = listOf(Song("detail:0", "노래", "가수")),
            ),
        )
        first.setMemorialVideo(null)
        first.setMemorialPhoto(null)

        val restored = viewModel(handle).currentForm()

        assertNull(restored.memorialVideoUrl)
        assertNull(restored.memorialThumbnailUrl)
        assertNull(restored.pickedMemorialPhotoUri)
        assertNull(restored.memorialPhotoUrl)
        assertNull(restored.displayMemorialPhotoUri())
        assertEquals(listOf("노래"), restored.memorialPlaylistSongs.map { it.title })
    }

    private fun memorialSavedStateHandle(): SavedStateHandle = SavedStateHandle(mapOf("initialType" to AfternoteType.MEMORIAL))

    private fun memorialPrefill(
        photoUrl: String? = null,
        videoUrl: String? = null,
        thumbnailUrl: String? = null,
        playlistSongs: List<Song> = emptyList(),
    ): EditorFormPrefill =
        EditorFormPrefill(
            content =
                EditorContentPrefill.Memorial(
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl,
                    photoUrl = photoUrl,
                    playlistSongs = playlistSongs,
                ),
            leaveMessageBlocks = emptyList(),
            receivers = emptyList(),
        )

    private fun viewModel(savedStateHandle: SavedStateHandle): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle = savedStateHandle,
            userRepository = repositoryProxy<UserRepository>(),
            afternoteRepository = repositoryProxy<AfternoteRepository>(),
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { _, _ -> error("미디어 업로드가 호출되면 안 됩니다") },
                ),
            errorReporter = repositoryProxy<ErrorReporter>(),
        )

    private inline fun <reified T> repositoryProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 예상하지 않았습니다") } as T
}
