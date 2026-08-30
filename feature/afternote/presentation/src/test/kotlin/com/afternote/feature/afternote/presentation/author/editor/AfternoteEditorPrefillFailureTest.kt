package com.afternote.feature.afternote.presentation.author.editor

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
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.afternoteEditorSavedStateHandle
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorError
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
import org.junit.Assert.assertFalse
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
