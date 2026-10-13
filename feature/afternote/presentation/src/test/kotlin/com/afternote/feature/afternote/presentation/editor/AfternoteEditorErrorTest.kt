package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.domain.usecase.editor.SaveAfternoteUseCase
import com.afternote.feature.afternote.presentation.NoopAuthorErrorReporter
import com.afternote.feature.afternote.presentation.afternoteAuthorUserReceiverRepository
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorErrorTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `네트워크 단절은 저장 오류 Network로 표시한다`() =
        assertSaveError(AfternoteFailure.NetworkUnavailable(IOException("timeout")), AfternoteEditorError.Network)

    @Test
    fun `영상 업로드 실패는 저장 미디어 오류로 표시한다`() =
        assertSaveError(
            AfternoteFailure.MediaSave(MediaKind.VIDEO, IOException("upload failed")),
            AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA),
        )

    @Test
    fun `사진 업로드 실패는 저장 미디어 오류로 표시한다`() =
        assertSaveError(
            AfternoteFailure.MediaSave(MediaKind.PHOTO, IOException("upload failed")),
            AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA),
        )

    @Test
    fun `그 밖의 저장 실패는 서버 오류로 표시한다`() = assertSaveError(IllegalStateException("failed"), AfternoteEditorError.Server)

    private fun assertSaveError(
        failure: Throwable,
        expected: AfternoteEditorError,
    ) = runTest(dispatcher) {
        val repository = FakeAfternoteRepository.strict().apply { onCreateSocial = { Result.failure(failure) } }
        val viewModel =
            AfternoteEditorViewModel(
                route = AfternoteRoute.EditorFlowRoute(initialType = AfternoteType.SOCIAL_NETWORK),
                savedStateHandle = SavedStateHandle(),
                userReceiverRepository = afternoteAuthorUserReceiverRepository(),
                afternoteRepository = repository,
                memorialThumbnailUploadRepository = MemorialThumbnailUploadRepository { error("unused") },
                resolveMemorialMediaForSave =
                    ResolveMemorialMediaForSaveUseCase(
                        MemorialMediaUploadRepository { _, _ -> Result.success(null) },
                    ),
                saveAfternoteUseCase = SaveAfternoteUseCase(repository),
                errorReporter = NoopAuthorErrorReporter,
            )
        viewModel.onIntent(
            AfternoteEditorIntent.Save(
                payload =
                    RegisterAfternotePayload(
                        serviceName = "인스타그램",
                        date = "2026.09.08",
                        accountId = "account",
                        password = "password",
                        processingMethods = listOf("계정 삭제"),
                    ),
                selectedReceiverIds = listOf(1L),
                memorialMedia = SaveAfternoteMemorialMedia(),
            ),
        )
        runCurrent()
        assertEquals(expected, viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isSaving)
    }
}
