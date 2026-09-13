package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editorFlowRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorProcessingMethodDefaultsTest {
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
    fun `신규 작성 추천은 한 번만 채우고 사용자가 전부 지우면 다시 넣지 않는다`() {
        val savedStateHandle = SavedStateHandle(mapOf("initialType" to AfternoteType.SOCIAL_NETWORK))
        val viewModel = viewModel(savedStateHandle)
        val defaults = listOf("게시물 내리기", "추모 게시물 올리기")

        viewModel.initializeProcessingMethodDefaults(AfternoteType.SOCIAL_NETWORK, defaults)

        assertEquals(listOf(1, 2), viewModel.currentForm().processingMethods.map { it.localId })
        assertEquals(defaults, viewModel.currentForm().processingMethods.map { it.text })

        viewModel.currentForm().processingMethods.forEach { viewModel.deleteProcessingMethod(it.localId) }
        viewModel.initializeProcessingMethodDefaults(AfternoteType.SOCIAL_NETWORK, defaults)

        assertTrue(viewModel.currentForm().processingMethods.isEmpty())
        assertTrue(viewModel(savedStateHandle).currentForm().processingMethods.isEmpty())
    }

    @Test
    fun `카테고리를 바꾸면 새 카테고리 추천을 채운다`() {
        val viewModel = viewModel(SavedStateHandle(mapOf("initialType" to AfternoteType.SOCIAL_NETWORK)))
        viewModel.initializeProcessingMethodDefaults(AfternoteType.SOCIAL_NETWORK, listOf("계정 삭제"))

        viewModel.setType(AfternoteType.GALLERY_AND_FILES)
        viewModel.initializeProcessingMethodDefaults(
            AfternoteType.GALLERY_AND_FILES,
            listOf("폴더 전송", "폴더 삭제"),
        )

        assertEquals(listOf("폴더 전송", "폴더 삭제"), viewModel.currentForm().processingMethods.map { it.text })
    }

    @Test
    fun `갤러리 생성 매퍼는 빈 처리 방법을 임의 문구로 대체하지 않는다`() {
        val input =
            AfternoteEditorFormMapper.buildCreateInput(
                type = AfternoteType.GALLERY_AND_FILES,
                payload = RegisterAfternotePayload(serviceName = "구글 포토", date = "2026.08.28"),
                selectedReceiverIds = listOf(1L),
                playlistSongs = emptyList(),
                memorialVideoUrl = null,
                memorialThumbnailUrl = null,
                memorialPhotoUrl = null,
            ) as CreateAfternoteInput.Gallery

        assertTrue(input.payload.processingMethods.isEmpty())
    }

    private fun viewModel(savedStateHandle: SavedStateHandle): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            route = savedStateHandle.editorFlowRoute(),
            savedStateHandle = savedStateHandle,
            userRepository = repositoryProxy(),
            afternoteRepository = repositoryProxy(),
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { _, _ -> error("미디어 업로드가 호출되면 안 됩니다") },
                ),
            errorReporter = repositoryProxy(),
        )

    private inline fun <reified T> repositoryProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 예상하지 않았습니다") } as T
}
