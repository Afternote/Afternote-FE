package com.afternote.feature.afternote.presentation.editor

import com.afternote.core.domain.error.FileUploadFailure
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.domain.usecase.editor.SaveAfternoteUseCase
import com.afternote.feature.afternote.presentation.NoopAuthorErrorReporter
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.afternoteAuthorUserReceiverRepository
import com.afternote.feature.afternote.presentation.afternoteEditorSavedStateHandle
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialPhoto
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editorFlowRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 업로드 크기 초과(서버 code 1803)를 일반 저장 실패와 가른다 (#1868).
 *
 * 크기 초과는 재시도로 풀리지 않는 유일한 업로드 실패다 — 같은 파일을 다시 보내면 서버가 같은
 * 코드로 다시 거절한다. 「저장에 실패했습니다」 로 뭉쳐 두면 사용자는 파일을 바꿔야 풀린다는 것도,
 * 기다렸다 다시 눌러 봐야 소용없다는 것도 알 수 없다.
 *
 * 진입은 공개 [AfternoteEditorIntent.Save] 하나이고, 실패는 실제 [ResolveMemorialMediaForSaveUseCase]
 * 를 통과해 온다 — 매핑만 직접 불러 확인하면 UseCase 가 cause 를 다르게 감싸도 초록으로 남는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorMediaSizeExceededTest {
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
    fun `영상이 한도를 넘으면 저장을 보내지 않고 크기 초과로 알린다`() {
        assertResolveFailure(
            failingKind = MediaKind.VIDEO,
            uploadFailure = fileSizeExceeded(),
            expected = AfternoteEditorError.MediaSizeExceeded,
        )
    }

    @Test
    fun `사진이 한도를 넘으면 저장을 보내지 않고 크기 초과로 알린다`() {
        assertResolveFailure(
            failingKind = MediaKind.PHOTO,
            uploadFailure = fileSizeExceeded(),
            expected = AfternoteEditorError.MediaSizeExceeded,
        )
    }

    /**
     * 갈래를 늘리면서 기존 안내를 뭉개지 않았는지 본다 — 사유를 확인하지 못한 업로드 실패는
     * data 계층이 치환하지 않고 원본 그대로 흘려보내므로, 여기서도 종전 일반 오류로 남아야 한다.
     */
    @Test
    fun `사유를 모르는 업로드 실패는 기존 저장 미디어 오류로 남는다`() {
        assertResolveFailure(
            failingKind = MediaKind.VIDEO,
            uploadFailure = IOException("connection reset"),
            expected = AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA),
        )
    }

    @Test
    fun `크기 초과는 일반 저장 실패와 다른 문구로 나가고 다시 시도를 권하지 않는다`() {
        assertEquals(
            R.string.afternote_editor_media_size_exceeded,
            AfternoteEditorError.MediaSizeExceeded.messageResId(),
        )
        assertNotEquals(
            "크기 초과가 일반 저장 실패와 같은 문구로 나가면 파일을 바꿔야 한다는 사실이 사라진다",
            AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA).messageResId(),
            AfternoteEditorError.MediaSizeExceeded.messageResId(),
        )
        assertFalse(
            "같은 파일을 다시 보내면 서버가 같은 코드로 거절한다 — 재시도 액션을 붙이면 안 된다",
            AfternoteEditorError.MediaSizeExceeded.offersMemorialThumbnailRetry(),
        )
    }

    /**
     * @param failingKind 업로드가 거절당하는 매체. 영상이 먼저 해석되므로 사진 갈래는 영상이
     *   통과한 뒤에야 도달한다 ([ResolveMemorialMediaForSaveUseCase]).
     */
    private fun assertResolveFailure(
        failingKind: MediaKind,
        uploadFailure: Throwable,
        expected: AfternoteEditorError,
    ) = runTest(dispatcher) {
        // strict fake — 저장 요청이 한 줄이라도 나가면 그 자리에서 터진다.
        val repository = FakeAfternoteRepository.strict()
        val viewModel = viewModel(repository, failingKind, uploadFailure)
        viewModel.onIntent(AfternoteEditorIntent.SetMemorialPhoto(PHOTO_URI))
        viewModel.onIntent(AfternoteEditorIntent.SetMemorialVideo(VIDEO_URI))

        val pickedMedia =
            viewModel.uiState.value.form
                .memorialMediaForSave()
        viewModel.onIntent(
            AfternoteEditorIntent.Save(
                payload = RegisterAfternotePayload(serviceName = "추억 노트", date = "2026.09.20"),
                selectedReceiverIds = emptyList(),
                memorialMedia = pickedMedia,
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(expected, state.error)
        assertFalse("업로드에서 끊겼는데 저장 중 표시가 남으면 등록 버튼이 잠긴 채로 굳는다", state.isSaving)
        assertNull(state.savedId)
        // 업로드에서 끊겼으니 저장 요청 자체가 나가지 않는다 — 나갔다면 payload 가 기록된다.
        assertTrue("미디어를 못 올렸는데 저장이 나가면 미디어 없는 기록이 확정된다", repository.memorialPayloads.isEmpty())
        // 고른 파일은 그대로 둔다 — 한도를 넘긴 쪽만 바꿔 다시 저장할 수 있어야 한다.
        assertEquals(PHOTO_URI, state.form.displayMemorialPhotoUri())
        assertEquals(VIDEO_URI, state.form.displayedMemorialVideo?.url)
    }

    private fun viewModel(
        repository: FakeAfternoteRepository,
        failingKind: MediaKind,
        uploadFailure: Throwable,
    ): AfternoteEditorViewModel {
        val savedStateHandle = afternoteEditorSavedStateHandle(initialType = AfternoteType.MEMORIAL)
        return AfternoteEditorViewModel(
            route = savedStateHandle.editorFlowRoute(),
            savedStateHandle = savedStateHandle,
            userReceiverRepository = afternoteAuthorUserReceiverRepository(),
            afternoteRepository = repository,
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { input, kind ->
                        when {
                            kind == failingKind -> Result.failure(uploadFailure)
                            input is MediaInput.Local -> Result.success("https://cdn.test/${kind.name.lowercase()}")
                            else -> Result.success(null)
                        }
                    },
                ),
            saveAfternoteUseCase = SaveAfternoteUseCase(repository),
            errorReporter = NoopAuthorErrorReporter,
        )
    }

    /** 서버 봉투의 code 1803 을 data 계층이 옮겨 놓은 모양 — presentation 은 이 타입만 보고 가른다. */
    private fun fileSizeExceeded(): FileUploadFailure.FileSizeExceeded =
        FileUploadFailure.FileSizeExceeded(IllegalStateException("code=1803 FILE_SIZE_EXCEEDED"))

    private fun EditorFormState.memorialMediaForSave(): SaveAfternoteMemorialMedia =
        SaveAfternoteMemorialMedia(
            memorialVideo = memorialVideo ?: EditableMemorialVideo.empty(),
            memorialPhoto = memorialPhoto ?: EditableMemorialPhoto.empty(),
        )

    private companion object {
        const val PHOTO_URI = "content://photos/portrait"
        const val VIDEO_URI = "content://videos/farewell"
    }
}
