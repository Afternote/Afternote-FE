package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.domain.usecase.editor.SaveAfternoteUseCase
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.afternoteEditorSavedStateHandle
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.lang.reflect.Proxy

/**
 * 수정 진입 prefill 조회 실패가 «빈 정상 에디터» 로 수렴하지 않는지에 대한 회귀 가드 (#705).
 *
 * 종전 구현은 실패 시 `isPrefillLoading = false` 만 세워 skeleton 을 걷었다 — 화면에는 신규 작성과
 * 구분되지 않는 빈 폼이 남고, 저장을 누르면 수정(PATCH)이 그 빈 값으로 나가 **기존 기록을 덮었다.**
 * 여기서 잠그는 셋은 오류 상태 유지 · 재시도 · 저장 차단이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorPrefillFailureTest {
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
    fun `prefill 실패는 빈 폼이 아니라 오류 상태로 남는다`() =
        runTest(dispatcher) {
            val reporter = RecordingErrorReporter()
            val viewModel = viewModel(alwaysFailingRepository(), reporter)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse("skeleton 에 갇히면 안 된다", state.isPrefillLoading)
            assertTrue("빈 폼으로 넘기지 말고 실패를 남긴다", state.isPrefillFailed)
            assertEquals(listOf("prefill_load"), reporter.recordedStages)
            assertEquals(listOf(IOException::class.java.name), reporter.recordedErrorTypes)
        }

    @Test
    fun `prefill 실패 상태에서는 저장이 나가지 않는다`() =
        runTest(dispatcher) {
            val repository = alwaysFailingRepository()
            val viewModel = viewModel(repository, RecordingErrorReporter())
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            viewModel.saveAfternote(
                payload = SAVE_PAYLOAD,
                selectedReceiverIds = listOf(7L),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
            runCurrent()

            assertTrue("기존 기록을 빈 값으로 덮는 수정이 나가면 안 된다", repository.updateCalls.isEmpty())
            assertEquals(AfternoteEditorError.PrefillUnavailable, viewModel.uiState.value.error)
        }

    @Test
    fun `다시 불러오기가 성공하면 오류를 걷고 폼을 채운다`() =
        runTest(dispatcher) {
            var shouldFail = true
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = {
                        if (shouldFail) Result.failure(IOException("상세 조회 실패")) else Result.success(DETAIL)
                    }
                }
            val viewModel = viewModel(repository, RecordingErrorReporter())
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            assertTrue(viewModel.uiState.value.isPrefillFailed)

            shouldFail = false
            viewModel.retryPrefill()
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse("성공한 재시도는 오류 상태를 걷는다", state.isPrefillFailed)
            assertNotNull("폼에 실을 prefill 이 도착해야 한다", state.pendingPrefill)
            assertEquals(2, repository.requestedDetailIds.size)
        }

    @Test
    fun `재시도가 성공한 뒤에는 저장이 다시 나간다`() =
        runTest(dispatcher) {
            var shouldFail = true
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = {
                        if (shouldFail) Result.failure(IOException("상세 조회 실패")) else Result.success(DETAIL)
                    }
                    onUpdate = { id, _ -> Result.success(id) }
                }
            val viewModel = viewModel(repository, RecordingErrorReporter())
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            shouldFail = false
            viewModel.retryPrefill()
            runCurrent()
            // 화면이 pendingPrefill 을 폼·TextFieldState 에 실은 뒤 통보하는 단계. 이걸 거쳐야
            // isPrefillLoading 이 내려간다 — 저장 가드가 «읽는 중» 도 막으므로 (#705) 여기서
            // 생략하면 폼이 아직 비어 있는 상태의 저장이 되어 정상 경로가 아니다.
            viewModel.onPrefillConsumed()
            runCurrent()
            viewModel.saveAfternote(
                payload = SAVE_PAYLOAD,
                selectedReceiverIds = listOf(7L),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
            runCurrent()

            assertEquals(listOf(ITEM_ID), repository.updateCalls.map { it.first })
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `prefill 실패 안내는 저장 실패와 다른 문구를 쓴다`() {
        assertEquals(
            R.string.afternote_editor_prefill_load_failed,
            AfternoteEditorError.PrefillUnavailable.messageResId(),
        )
    }

    @Test
    fun `prefill 실패 동안에는 이탈 기준선을 잡지 않는다`() {
        assertTrue(
            shouldDeferEditorBaselineCapture(
                isPrefillLoading = false,
                isProcessingMethodDefaultsInitializing = false,
                isPrefillFailed = true,
            ),
        )
    }

    @Test
    fun `prefill 이 아직 도착하지 않았으면 저장이 나가지 않는다`() =
        runTest(dispatcher) {
            // 상세 GET 이 영영 끝나지 않는 저장소 — skeleton 이 떠 있는 창을 그대로 붙잡아 둔다.
            val neverCompleting =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { awaitCancellation() }
                    onUpdate = { _, _ -> error("prefill 이 도착하기 전에 수정이 나가면 안 됩니다") }
                }
            val viewModel = viewModel(neverCompleting, RecordingErrorReporter())
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            assertTrue("전제: 아직 prefill 을 읽는 중이어야 한다", viewModel.uiState.value.isPrefillLoading)
            assertFalse("이 경로는 실패가 아니다", viewModel.uiState.value.isPrefillFailed)

            viewModel.saveAfternote(
                payload = SAVE_PAYLOAD,
                selectedReceiverIds = listOf(7L),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
            runCurrent()

            assertTrue(
                "느린 상세 GET 을 앞질러 저장하면 기존 기록이 빈 폼으로 덮인다",
                neverCompleting.updateCalls.isEmpty(),
            )
            assertEquals(
                "아직 읽는 중인 사용자에게 «불러오지 못했습니다» 라고 말하면 사실과 다르다",
                AfternoteEditorError.PrefillNotReady,
                viewModel.uiState.value.error,
            )
        }

    @Test
    fun `읽는 중과 실패는 서로 다른 문구로 갈린다`() {
        assertEquals(
            R.string.afternote_editor_prefill_not_ready,
            AfternoteEditorError.PrefillNotReady.messageResId(),
        )
        assertNotEquals(
            "저장을 막는 이유가 같아도 사용자에게 할 말이 다르다",
            AfternoteEditorError.PrefillUnavailable.messageResId(),
            AfternoteEditorError.PrefillNotReady.messageResId(),
        )
    }

    /**
     * `retryPrefill` 의 널 가지를 잠근다.
     *
     * 신규 작성 진입은 itemId 가 없어 prefill 자체가 돌지 않고, 화면도 실패 상태에서만 재시도 버튼을
     * 그리므로 이 호출은 실제로는 도달하지 않는다. 그래도 함수가 public 이라 밖에서 부를 수 있고,
     * 조기 반환을 지우면 조회가 나가거나 강제 언랩이 필요해진다 — 그 회귀를 여기서 잡는다.
     */
    @Test
    fun `신규 작성 진입에서 재시도를 불러도 상세 조회가 나가지 않는다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { error("신규 작성에는 읽어 올 기존 기록이 없습니다") }
                }
            val viewModel =
                AfternoteEditorViewModel(
                    savedStateHandle =
                        afternoteEditorSavedStateHandle(initialType = AfternoteType.SOCIAL_NETWORK, itemId = null),
                    userRepository = unusedProxy<UserRepository>(),
                    afternoteRepository = repository,
                    memorialThumbnailUploadRepository =
                        MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
                    resolveMemorialMediaForSave =
                        ResolveMemorialMediaForSaveUseCase(
                            MemorialMediaUploadRepository { input, _ ->
                                if (input == MediaInput.None) Result.success(null) else error("미디어 업로드가 호출되면 안 됩니다")
                            },
                        ),
                    saveAfternoteUseCase = SaveAfternoteUseCase(repository),
                    errorReporter = RecordingErrorReporter(),
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()
            assertFalse("전제: 신규 작성은 prefill 을 돌리지 않는다", viewModel.uiState.value.isPrefillLoading)

            viewModel.retryPrefill()
            runCurrent()

            assertFalse("재시도가 신규 작성에서 skeleton 을 세우면 안 된다", viewModel.uiState.value.isPrefillLoading)
            assertFalse("실패 상태로도 넘어가면 안 된다", viewModel.uiState.value.isPrefillFailed)
        }

    @Test
    fun `prefill 진행 중에는 등록 버튼이 잠긴다`() {
        assertFalse(
            "skeleton 이 떠 있는 동안 등록이 눌리면 ViewModel 가드까지 가서야 막힌다",
            isEditorSubmitEnabled(isSaving = false, isPrefillFailed = false, isPrefillLoading = true),
        )
        assertTrue(
            "prefill 이 끝난 정상 편집 상태에서는 잠기지 않는다",
            isEditorSubmitEnabled(isSaving = false, isPrefillFailed = false, isPrefillLoading = false),
        )
    }

    private fun alwaysFailingRepository(): FakeAfternoteRepository =
        FakeAfternoteRepository.strict().apply {
            onGetDetail = { Result.failure(IOException("상세 조회 실패")) }
            onUpdate = { _, _ -> error("prefill 을 못 읽은 채 수정이 나가면 안 됩니다") }
        }

    private fun viewModel(
        repository: FakeAfternoteRepository,
        errorReporter: ErrorReporter,
    ): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle = editorSavedStateHandle(),
            userRepository = unusedProxy<UserRepository>(),
            afternoteRepository = repository,
            memorialThumbnailUploadRepository =
                MemorialThumbnailUploadRepository { error("썸네일 업로드가 호출되면 안 됩니다") },
            resolveMemorialMediaForSave =
                ResolveMemorialMediaForSaveUseCase(
                    // 이 테스트의 폼에는 추억 노트 미디어가 없다 — None 만 해석하고 업로드는 닫아 둔다.
                    MemorialMediaUploadRepository { input, _ ->
                        if (input == MediaInput.None) {
                            Result.success(null)
                        } else {
                            error("미디어 업로드가 호출되면 안 됩니다")
                        }
                    },
                ),
            saveAfternoteUseCase = SaveAfternoteUseCase(repository),
            errorReporter = errorReporter,
        )

    private fun editorSavedStateHandle(): SavedStateHandle =
        afternoteEditorSavedStateHandle(
            initialType = AfternoteType.SOCIAL_NETWORK,
            itemId = ITEM_ID,
        )

    private inline fun <reified T> unusedProxy(): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ -> error("${T::class.java.simpleName}.${method.name} 호출은 이 테스트에서 예상하지 않았습니다") } as T

    /**
     * [ErrorReporter] 는 예외를 redact 하므로 원문으로는 단언할 수 없다 — 남는 것은 `afternote_stage` 와
     * 창구가 붙이는 `error_type` 뿐이다.
     */
    private class RecordingErrorReporter : ErrorReporter {
        val recordedStages = mutableListOf<String>()
        val recordedErrorTypes = mutableListOf<String>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            attributes["afternote_stage"]?.let(recordedStages::add)
            attributes["error_type"]?.let(recordedErrorTypes::add)
        }
    }

    private companion object {
        const val ITEM_ID = 42L

        /** 검증(SOCIAL_NETWORK 계정 필수)을 통과하는 최소 페이로드 — 막히는 지점이 prefill 가드임을 분명히 한다. */
        val SAVE_PAYLOAD =
            RegisterAfternotePayload(
                serviceName = "인스타그램",
                date = "2026.08.30",
                accountId = "account",
                password = "password",
            )

        val DETAIL =
            Detail(
                id = ITEM_ID,
                serviceName = "인스타그램",
                timestamps = DetailTimestamps(updatedAt = "2026-08-30"),
                receivers = emptyList(),
                leaveMessageBlocks = listOf(LeaveMessageBlock(title = null, body = "부탁해")),
                content =
                    DetailContent.SocialNetwork(
                        credentials = DetailCredentials(id = "account", password = "password"),
                        processingMethods = listOf("계정 삭제"),
                    ),
            )
    }
}
