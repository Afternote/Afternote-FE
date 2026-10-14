package com.afternote.feature.afternote.presentation.editor

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.domain.usecase.editor.SaveAfternoteUseCase
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 발행 수정 화면 복원 뒤 재조회가 미저장 입력을 보존하고 저장 기준은 갱신하는지 검증한다. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AfternoteEditorProcessDeathPrefillTest {
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
    fun `복원된 편집이 있으면 상세 재조회가 프리필을 발행하지 않는다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(serverDetail()) }
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertEquals(
                "복원된 폼은 사용자가 고친 제목을 그대로 들고 있다",
                EDITED_SERVICE,
                viewModel.uiState.value.form.selectedService,
            )
            assertNull(
                "재조회가 프리필을 발행하면 화면이 그것을 실어 사용자의 편집이 사라진다",
                viewModel.uiState.value.pendingPrefill,
            )
        }

    @Test
    fun `프리필을 막아도 복원된 편집 그대로 저장이 나간다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(serverDetail()) }
                    onUpdate = { id, _ -> Result.success(id) }
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            assertFalse(
                "프리필을 싣지 않았으니 skeleton 도 여기서 걷혀야 한다",
                viewModel.uiState.value.isPrefillLoading,
            )

            // 화면이 복원된 폼 값을 그대로 담아 보내는 저장이다.
            viewModel.onIntent(
                AfternoteEditorIntent.Save(
                    payload =
                        RegisterAfternotePayload(
                            serviceName = EDITED_SERVICE,
                            date = "2026-08-30",
                            processingMethods = listOf(SERVER_PROCESSING_METHOD),
                        ),
                    selectedReceiverIds = emptyList(),
                    memorialMedia = SaveAfternoteMemorialMedia(),
                ),
            )
            runCurrent()

            val updated = repository.updateCalls.single().second
            assertEquals("사용자가 고친 제목이 그대로 나간다", EDITED_SERVICE, updated.title)
            assertNull(
                "복원된 폼이 서버 값을 그대로 들고 있으므로 안 건드린 필드는 실리지 않는다 (#1617)",
                updated.processingMethods,
            )
        }

    @Test
    fun `프리필이 실린 적 없는 복원은 여전히 프리필을 받는다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(serverDetail()) }
                }
            val viewModel = viewModel(repository, prefillSeeded = false)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val prefill = viewModel.uiState.value.pendingPrefill
            assertNotNull("서버 값을 본 적 없는 폼은 프리필로 채워야 한다", prefill)
            assertEquals(
                SERVER_SERVICE,
                (prefill?.content as EditorContentPrefill.Gallery).serviceName,
            )
        }

    @Test
    fun `스냅샷이 깨져 빈 폼으로 떨어진 복원은 프리필을 받는다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(serverDetail()) }
                }
            val viewModel = viewModel(repository, snapshot = "{ 이건 EditorFormSnapshot 이 아니다 }")
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val prefill = viewModel.uiState.value.pendingPrefill
            assertNotNull("빈 폼으로 떨어졌으면 프리필을 막으면 안 된다", prefill)
            assertEquals(
                SERVER_SERVICE,
                (prefill?.content as EditorContentPrefill.Gallery).serviceName,
            )
        }

    @Test
    fun `표식만 남고 폼 스냅샷이 없으면 프리필을 받는다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(serverDetail()) }
                }
            val viewModel = viewModel(repository, snapshot = null)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            val prefill = viewModel.uiState.value.pendingPrefill
            assertNotNull("실제 폼 복원이 없으면 서버 값을 받아야 한다", prefill)
            assertEquals(SERVER_SERVICE, (prefill?.content as EditorContentPrefill.Gallery).serviceName)
        }

    private fun serverDetail() =
        Detail(
            id = EDIT_ID,
            serviceName = SERVER_SERVICE,
            timestamps = DetailTimestamps(updatedAt = "2026-08-30"),
            receivers = emptyList(),
            leaveMessageBlocks = emptyList(),
            content = DetailContent.Gallery(processingMethods = listOf(SERVER_PROCESSING_METHOD)),
        )

    /** 서버 prefill 적용 뒤 제목만 고친 폼. */
    private fun restoredSnapshot(): String =
        """
        {"type":"GALLERY_AND_FILES","selectedService":"$EDITED_SERVICE","receivers":[],
        "processingMethods":[{"localId":0,"text":"$SERVER_PROCESSING_METHOD"}],"memorialPlaylistSongs":[]}
        """.trimIndent().replace("\n", "")

    private fun viewModel(
        afternoteRepository: FakeAfternoteRepository,
        prefillSeeded: Boolean = true,
        snapshot: String? = restoredSnapshot(),
    ): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            route = AfternoteRoute.EditorFlowRoute(itemId = EDIT_ID, initialType = AfternoteType.GALLERY_AND_FILES),
            savedStateHandle =
                SavedStateHandle(
                    buildMap {
                        put("initialType", AfternoteType.GALLERY_AND_FILES)
                        put("itemId", EDIT_ID)
                        if (snapshot != null) put("editor_form_snapshot_v6", snapshot)
                        // 화면이 프리필을 폼에 실을 때 ViewModel 이 같은 번들에 남기는 표식.
                        if (prefillSeeded) put("editor_prefill_seeded_item_id", EDIT_ID)
                    },
                ),
            userReceiverRepository = FakeUserReceiverRepository.strict(),
            afternoteRepository = afternoteRepository,
            saveAfternoteUseCase = SaveAfternoteUseCase(afternoteRepository),
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
        const val EDITED_SERVICE = "사용자가 고친 제목"
        const val SERVER_SERVICE = "구글 포토"
        const val SERVER_PROCESSING_METHOD = "파일 전달"
    }
}
