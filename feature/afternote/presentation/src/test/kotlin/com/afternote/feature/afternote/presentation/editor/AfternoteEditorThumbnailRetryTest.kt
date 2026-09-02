package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

/**
 * 추모 영상 썸네일 실패의 복구 경로 (#1550).
 *
 * 종전에는 실패를 한 번 알리고 끝냈다. 추출은 `LaunchedEffect(videoUrl)` 키라 같은 영상이 폼에 있는
 * 한 다시 돌지 않고, 저장하면 썸네일 없는 영상이 확정되며, 재편집으로 들어와도 원격 URL 이라 추출을
 * 건너뛴다 — 영상을 처음부터 다시 고르는 것 말고는 되돌릴 방법이 없었다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorThumbnailRetryTest {
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
    fun `업로드가 실패하면 재시도할 수 있다고 알린다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(uploads = ThumbnailUploads(failures = 1))
            observeUiState(viewModel)
            viewModel.setMemorialVideo(LOCAL_VIDEO)

            viewModel.uploadMemorialThumbnail(JPEG_BYTES)
            advanceUntilIdle()

            assertEquals(
                AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.THUMBNAIL),
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `재시도는 영상 재선택 없이 같은 바이트를 다시 올린다`() =
        runTest(dispatcher) {
            val uploads = ThumbnailUploads(failures = 1)
            val viewModel = viewModel(uploads)
            observeUiState(viewModel)
            viewModel.setMemorialVideo(LOCAL_VIDEO)
            viewModel.uploadMemorialThumbnail(JPEG_BYTES)
            advanceUntilIdle()

            viewModel.retryMemorialThumbnail()
            advanceUntilIdle()

            assertEquals("두 번째 시도까지 두 번 올린다", 2, uploads.attempts)
            assertEquals(UPLOADED_URL, viewModel.uiState.value.pendingThumbnailUrl)
            // 고른 영상은 그대로다 — 되돌리려고 영상을 다시 고르게 하지 않는다.
            assertEquals(LOCAL_VIDEO, viewModel.currentForm().displayedMemorialVideo?.url)
        }

    @Test
    fun `추출 실패는 무음이 아니라 재시도 가능한 오류로 알린다`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            observeUiState(viewModel)
            viewModel.setMemorialVideo(LOCAL_VIDEO)

            viewModel.onMemorialThumbnailExtractionFailed(IllegalStateException("no frame"))
            advanceUntilIdle()

            assertEquals(
                AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.THUMBNAIL_EXTRACT),
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `추출 실패의 재시도는 올릴 바이트가 없으므로 추출부터 다시 돌린다`() =
        runTest(dispatcher) {
            val uploads = ThumbnailUploads()
            val viewModel = viewModel(uploads)
            observeUiState(viewModel)
            viewModel.setMemorialVideo(LOCAL_VIDEO)
            viewModel.onMemorialThumbnailExtractionFailed(IllegalStateException("no frame"))
            val before = viewModel.uiState.value.memorialThumbnailRetryToken

            viewModel.retryMemorialThumbnail()
            advanceUntilIdle()

            // 토큰이 바뀌면 화면이 같은 영상에서 프레임 추출을 다시 발화한다.
            assertEquals(before + 1, viewModel.uiState.value.memorialThumbnailRetryToken)
            assertEquals("올릴 바이트가 없으니 업로드는 부르지 않는다", 0, uploads.attempts)
        }

    @Test
    fun `영상을 새로 고르면 이전 실패는 무효가 된다`() =
        runTest(dispatcher) {
            val uploads = ThumbnailUploads(failures = 1)
            val viewModel = viewModel(uploads)
            observeUiState(viewModel)
            viewModel.setMemorialVideo(LOCAL_VIDEO)
            viewModel.uploadMemorialThumbnail(JPEG_BYTES)
            advanceUntilIdle()

            viewModel.setMemorialVideo("content://videos/another")
            viewModel.retryMemorialThumbnail()
            advanceUntilIdle()

            // 남은 바이트로 다시 올리면 다른 영상의 그림이 붙는다.
            assertEquals(1, uploads.attempts)
            assertNull(viewModel.uiState.value.pendingThumbnailUrl)
        }

    /**
     * `uiState` 는 `WhileSubscribed` 로 공유돼 구독자가 없으면 초기값에 멈춘다. 관찰을 붙여야
     * 상태 변화를 읽을 수 있고, 구독은 즉시 붙어야 하므로 Unconfined 로 띄운다.
     */
    private fun TestScope.observeUiState(viewModel: AfternoteEditorViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun viewModel(uploads: ThumbnailUploads = ThumbnailUploads()): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("initialType" to AfternoteType.MEMORIAL)),
            userRepository = repositoryProxy<UserRepository>(),
            afternoteRepository = repositoryProxy<AfternoteRepository>(),
            memorialThumbnailUploadRepository = uploads,
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { _, _ -> error("미디어 업로드가 호출되면 안 됩니다") },
                ),
            errorReporter = RecordingErrorReporter(),
        )

    /** 앞의 [failures] 번은 실패하고 그 뒤로는 성공한다. 시도 횟수를 세어 재시도가 실제로 도는지 본다. */
    private class ThumbnailUploads(
        private val failures: Int = 0,
    ) : MemorialThumbnailUploadRepository {
        var attempts: Int = 0
            private set

        override suspend fun uploadThumbnail(jpegBytes: ByteArray): Result<String> {
            attempts += 1
            return if (attempts <= failures) {
                Result.failure(IllegalStateException("S3 upload failed"))
            } else {
                Result.success(UPLOADED_URL)
            }
        }
    }

    /** 실패 경로가 계측을 부르므로 삼키는 구현이 필요하다 — 이 테스트가 보는 건 사용자 복구 경로다. */
    private class RecordingErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private inline fun <reified T> repositoryProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 예상하지 않았습니다") } as T

    private companion object {
        const val LOCAL_VIDEO = "content://videos/farewell"
        const val UPLOADED_URL = "https://cdn.test/thumb.jpg"
        val JPEG_BYTES = byteArrayOf(1, 2, 3)
    }
}
