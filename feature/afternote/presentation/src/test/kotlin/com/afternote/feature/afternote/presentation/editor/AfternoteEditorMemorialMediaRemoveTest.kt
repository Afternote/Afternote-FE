package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

/**
 * 추억 노트 미디어 슬롯의 삭제 경로 (#1114, #1597) —
 * `removeMemorialPhoto()`/`removeMemorialVideo()`.
 *
 * 삭제는 세터의 null 인자가 아니라 이름 있는 연산이다(#1717). 삭제는 로컬 첨부와 서버 원본을
 * 함께 비우고, 그 상태는 SavedState 스냅샷에 실린다. 서버 삭제는 저장 시 명시적 null 로 나간다(#1597).
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

        viewModel.removeMemorialVideo()

        assertNull(viewModel.currentForm().displayedMemorialVideo)
    }

    @Test
    fun `사진 삭제는 로컬 픽과 서버 원본을 함께 비운다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.applyPrefill(memorialPrefill(photoUrl = "https://cdn.test/portrait.jpg"))
        viewModel.setMemorialPhoto("content://photos/replacement")
        assertEquals("content://photos/replacement", viewModel.currentForm().displayMemorialPhotoUri())

        viewModel.removeMemorialPhoto()

        assertNull(viewModel.currentForm().pickedMemorialPhotoUri)
        assertNull(viewModel.currentForm().memorialPhotoUrl)
        assertNull(viewModel.currentForm().displayMemorialPhotoUri())
    }

    @Test
    fun `영상 삭제는 로컬 픽과 서버 원본과 썸네일을 함께 비운다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.applyPrefill(
            memorialPrefill(
                videoUrl = "https://cdn.test/farewell.mp4",
                thumbnailUrl = "https://cdn.test/server-thumbnail.jpg",
            ),
        )
        viewModel.setMemorialVideo("content://videos/replacement")
        viewModel.setMemorialThumbnail("https://cdn.test/local-thumbnail.jpg")

        viewModel.removeMemorialVideo()

        assertNull(viewModel.currentForm().displayedMemorialVideo)
        assertFalse(viewModel.currentForm().canRemoveMemorialVideo)
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

        viewModel.removeMemorialPhoto()
        viewModel.removeMemorialVideo()

        assertNull(viewModel.currentForm().pickedMemorialPhotoUri)
        assertNull(viewModel.currentForm().memorialPhotoUrl)
        assertNull(viewModel.currentForm().displayMemorialPhotoUri())
        assertNull(viewModel.currentForm().displayedMemorialVideo?.url)
        assertNull(viewModel.currentForm().displayedMemorialVideo?.thumbnailUrl)
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
        first.removeMemorialVideo()
        first.removeMemorialPhoto()

        val restored = viewModel(handle).currentForm()

        assertNull(restored.displayedMemorialVideo)
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
            userReceiverRepository = repositoryProxy<UserReceiverRepository>(),
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
