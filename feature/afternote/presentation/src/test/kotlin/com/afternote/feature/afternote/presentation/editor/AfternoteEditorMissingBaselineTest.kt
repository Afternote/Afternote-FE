package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 기준 스냅샷 없이 수정 저장이 나가지 않는지에 대한 회귀 가드 (#1617).
 *
 * 상세 조회가 실패하면 폼이 **비어 있는 채로** 열린다. 그 상태에서 저장하면 「안 건드림」과 「전부
 * 지움」을 가릴 기준이 없어, 빈 처리 방법·수신자·남기실 말씀이 그대로 삭제 지시로 나간다 — 이 이슈가
 * 잡으려던 바로 그 사고다. 게다가 제목만 채우면 GALLERY·MEMORIAL 은 Validator 도 통과한다.
 *
 * 그래서 저장을 **시작하지 않고** 사용자에게 다시 불러오라고 알린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorMissingBaselineTest {
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
    fun `상세를 못 받은 채 저장하면 요청을 보내지 않고 알린다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.failure(IOException("상세 조회 실패")) }
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.saveAfternote(
                payload = RegisterAfternotePayload(serviceName = "구글 포토", date = "2026-08-30"),
                selectedReceiverIds = emptyList(),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
            runCurrent()

            assertTrue("기준 없는 수정은 서버로 나가면 안 된다", repository.updateCalls.isEmpty())
            assertEquals(
                AfternoteEditorError.PrefillUnavailable,
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `상세를 받았으면 저장이 정상으로 나간다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(galleryDetail()) }
                    onUpdate = { id, _ -> Result.success(id) }
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            // 화면이 prefill 을 폼에 반영했다는 통보. 이걸 빼면 저장이 «아직 읽는 중»
            // ([AfternoteEditorError.PrefillNotReady]) 으로 막혀 기준 판정까지 가지 못한다 (#705).
            viewModel.onPrefillConsumed()
            runCurrent()

            viewModel.saveAfternote(
                payload = RegisterAfternotePayload(serviceName = "새 제목", date = "2026-08-30"),
                selectedReceiverIds = emptyList(),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
            runCurrent()

            assertEquals(1, repository.updateCalls.size)
            assertEquals(
                "새 제목",
                repository.updateCalls
                    .single()
                    .second.title,
            )
        }

    /**
     * 저장 차단 안내는 저장 실패 문구와 달라야 한다 — 사용자가 할 일이 다르다.
     *
     * 문구는 #705 가 세운 prefill 조회 실패 문구를 함께 쓴다. 「기준이 없다」와 「기존 내용을
     * 읽지 못했다」는 사용자에게 같은 사실이고 할 일도 같다 — 다시 불러오는 것이다.
     */
    @Test
    fun `기준 없음은 저장 실패와 다른 문구로 안내한다`() {
        assertEquals(
            R.string.afternote_editor_prefill_load_failed,
            AfternoteEditorError.PrefillUnavailable.messageResId(),
        )
    }

    private fun galleryDetail() =
        Detail(
            id = EDIT_ID,
            serviceName = "구글 포토",
            timestamps = DetailTimestamps(updatedAt = "2026-08-30"),
            receivers = emptyList(),
            leaveMessageBlocks = emptyList(),
            content = DetailContent.Gallery(processingMethods = listOf("파일 전달")),
        )

    private fun viewModel(afternoteRepository: FakeAfternoteRepository): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "initialType" to AfternoteType.GALLERY_AND_FILES,
                        "itemId" to EDIT_ID,
                    ),
                ),
            userRepository = FakeUserRepository.strict(),
            afternoteRepository = afternoteRepository,
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    MemorialMediaUploadRepository { _, _ -> Result.success(null) },
                ),
            errorReporter = NoopErrorReporter,
        )

    private object NoopErrorReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private companion object {
        const val EDIT_ID = 73L
    }
}
