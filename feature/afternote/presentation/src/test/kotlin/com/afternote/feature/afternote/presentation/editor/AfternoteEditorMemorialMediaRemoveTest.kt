package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.domain.usecase.editor.SaveAfternoteUseCase
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
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
 * 추억 노트 미디어 슬롯의 삭제 경로 (#1114) — `setMemorialPhoto(null)`/`setMemorialVideo(null)`.
 *
 * 삭제는 새 세터가 아니라 기존 nullable 세터의 null 인자로 표현된다. 여기서 굳히는 규칙:
 * 영상 삭제는 파생 썸네일을 함께 비우고, 사진 삭제는 로컬 픽만 지워 서버 사진 표시로 되돌리며,
 * 삭제된 상태는 SavedState 스냅샷에도 그대로 실린다.
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
    fun `영상을 삭제하면 파생 썸네일도 함께 비운다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.setMemorialVideo("content://videos/farewell")
        viewModel.setMemorialThumbnail("https://cdn.test/thumbnail.jpg")

        viewModel.setMemorialVideo(null)

        assertNull(viewModel.currentForm().displayedMemorialVideo)
    }

    @Test
    fun `사진 삭제는 로컬 픽만 지우고 서버 사진 표시로 되돌린다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.applyPrefill(memorialPrefill(photoUrl = "https://cdn.test/portrait.jpg"))
        viewModel.setMemorialPhoto("content://photos/replacement")
        assertEquals("content://photos/replacement", viewModel.currentForm().displayMemorialPhotoUri())

        viewModel.setMemorialPhoto(null)

        // 서버 사진은 BE 수정 계약이 삭제를 표현하지 못해 폼에서도 지우지 않는다 —
        // 표시·저장 모두 서버 사진 유지로 일관되게 남는다.
        assertNull(viewModel.currentForm().pickedMemorialPhotoUri)
        assertEquals("https://cdn.test/portrait.jpg", viewModel.currentForm().memorialPhotoUrl)
        assertEquals("https://cdn.test/portrait.jpg", viewModel.currentForm().displayMemorialPhotoUri())
    }

    @Test
    fun `신규 작성에서 사진을 삭제하면 슬롯이 완전히 빈다`() {
        val viewModel = viewModel(memorialSavedStateHandle())
        viewModel.setMemorialPhoto("content://photos/new")

        viewModel.setMemorialPhoto(null)

        assertNull(viewModel.currentForm().displayMemorialPhotoUri())
    }

    @Test
    fun `삭제된 첨부는 프로세스 복원 후에도 비어 있다`() {
        val handle = memorialSavedStateHandle()
        val first = viewModel(handle)
        first.setMemorialVideo("content://videos/farewell")
        first.setMemorialPhoto("content://photos/new")
        first.setMemorialVideo(null)
        first.setMemorialPhoto(null)

        val restored = viewModel(handle).currentForm()

        assertNull(restored.displayedMemorialVideo)
        assertNull(restored.pickedMemorialPhotoUri)
    }

    private fun memorialSavedStateHandle(): SavedStateHandle = SavedStateHandle(mapOf("initialType" to AfternoteType.MEMORIAL))

    private fun memorialPrefill(photoUrl: String?): EditorFormPrefill =
        EditorFormPrefill(
            content =
                EditorContentPrefill.Memorial(
                    videoUrl = null,
                    thumbnailUrl = null,
                    photoUrl = photoUrl,
                    playlistSongs = emptyList(),
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
            saveAfternoteUseCase = SaveAfternoteUseCase(repositoryProxy<AfternoteRepository>()),
            errorReporter = repositoryProxy<ErrorReporter>(),
        )

    private inline fun <reified T> repositoryProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 예상하지 않았습니다") } as T
}
