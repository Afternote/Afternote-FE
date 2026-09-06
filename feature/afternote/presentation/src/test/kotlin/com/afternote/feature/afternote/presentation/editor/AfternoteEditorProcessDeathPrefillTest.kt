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
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
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

/**
 * 프로세스 사망 복원이 사용자의 미저장 편집을 서버 값으로 덮지 않는지 고정한다 (#1732).
 *
 * 복원 경로는 두 갈래가 겹친다: `SavedStateHandle` 의 폼 스냅샷이 편집분을 되살리고, 곧이어
 * `init` 의 상세 재조회가 끝난다. 종전에는 그 재조회가 **폼이 더러운지 묻지 않고** `pendingPrefill`
 * 을 다시 발행해, 화면이 그것을 폼·`TextFieldState` 에 실으면서 되살린 편집을 서버 값으로 덮었다.
 *
 * 결정은 「덮지 않는다」다. 서버 값으로 되돌릴지 묻는 화면은 시안도 문구도 없고, 사용자가 쓴 것을
 * 조용히 버리는 쪽이 더 나쁘다. 재조회 자체는 그대로 돈다 — 기준 스냅샷(#1617)과 카테고리는
 * 서버가 아는 값이어야 하고, 막는 것은 「폼에 덮어쓰기」 하나다.
 *
 * 가드가 보는 것은 **폼 스냅샷 + 「프리필이 실렸다」 표식**이지 폼 값의 비교가 아니다. 계정 정보와
 * 남기실 말씀은 화면(`rememberTextFieldState`·`rememberSaveable`)이 소유해 폼에 없으므로, 값 비교로
 * 갈랐다면 비밀번호만 고친 복원이 「폼이 같다」로 읽혀 그대로 덮였을 것이다.
 */
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
                viewModel.currentForm().selectedService,
            )
            assertNull(
                "재조회가 프리필을 발행하면 화면이 그것을 실어 사용자의 편집이 사라진다",
                viewModel.uiState.value.pendingPrefill,
            )
        }

    /**
     * 프리필을 싣지 않으면 화면의 `onPrefillConsumed` 통보도 오지 않는다. skeleton 을 걷고 기준
     * 스냅샷을 세우는 일은 그래서 재조회 쪽이 직접 해야 한다 — 안 그러면 복원된 사용자는 「곧
     * 도착합니다」(`PrefillNotReady`) 에 막혀 영영 저장하지 못한다.
     */
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
            viewModel.saveAfternote(
                payload =
                    RegisterAfternotePayload(
                        serviceName = EDITED_SERVICE,
                        date = "2026-08-30",
                        processingMethods = listOf(SERVER_PROCESSING_METHOD),
                    ),
                selectedReceiverIds = emptyList(),
                memorialMedia = SaveAfternoteMemorialMedia(),
            )
            runCurrent()

            val updated = repository.updateCalls.single().second
            assertEquals("사용자가 고친 제목이 그대로 나간다", EDITED_SERVICE, updated.title)
            assertNull(
                "복원된 폼이 서버 값을 그대로 들고 있으므로 안 건드린 필드는 실리지 않는다 (#1617)",
                updated.processingMethods,
            )
        }

    /**
     * 프리필이 실리기 전에 죽은 복원은 **막지 않는다**.
     *
     * 그 폼은 사용자가 skeleton 위에서 친 몇 글자뿐이고 서버 값을 본 적이 없다. 여기서 프리필까지
     * 막으면 반쪽짜리 폼이 기준 스냅샷과 짝지어져, 안 건드린 필드가 「전부 지움」으로 나간다 —
     * #705·#1617 이 막은 바로 그 사고다.
     */
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

    /**
     * **스냅샷 문자열은 남았는데 디코딩이 실패한 복원도 프리필을 받아야 한다.**
     *
     * 가드가 「키가 있는가」만 보면 이 조합(표식 있음 + 문자열 있음 + 디코딩 실패)에서 참이 되어
     * 프리필이 막힌다. 그런데 폼은 실패를 삼켜 **빈 기본값**이라, 그 빈 폼이 새 기준 스냅샷과
     * 짝지어져 안 건드린 필드가 전부 「지움」으로 나간다 — #705·#1617 이 막은 그 사고다.
     *
     * 키 접미사(`_v4`)를 올리지 않은 채 스냅샷 스키마가 바뀌는 순간 열리는 잠복 경로다.
     */
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

    private fun serverDetail() =
        Detail(
            id = EDIT_ID,
            serviceName = SERVER_SERVICE,
            timestamps = DetailTimestamps(updatedAt = "2026-08-30"),
            receivers = emptyList(),
            leaveMessageBlocks = emptyList(),
            content = DetailContent.Gallery(processingMethods = listOf(SERVER_PROCESSING_METHOD)),
        )

    /**
     * 프로세스 사망 전에 저장돼 있던 폼 스냅샷.
     *
     * 프리필이 실린 뒤 사용자가 **제목만** 고친 상태다 — 처리 방법은 서버 값 그대로다.
     */
    private fun restoredSnapshot(): String =
        """
        {"type":"GALLERY_AND_FILES","selectedService":"$EDITED_SERVICE","receivers":[],
        "processingMethods":[{"localId":0,"text":"$SERVER_PROCESSING_METHOD"}],"memorialPlaylistSongs":[]}
        """.trimIndent().replace("\n", "")

    private fun viewModel(
        afternoteRepository: FakeAfternoteRepository,
        prefillSeeded: Boolean = true,
        snapshot: String = restoredSnapshot(),
    ): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle =
                SavedStateHandle(
                    buildMap {
                        put("initialType", AfternoteType.GALLERY_AND_FILES)
                        put("itemId", EDIT_ID)
                        put("editor_form_snapshot_v4", snapshot)
                        // 화면이 프리필을 폼에 실을 때 ViewModel 이 같은 번들에 남기는 표식.
                        if (prefillSeeded) put("editor_prefill_seeded_item_id", EDIT_ID)
                    },
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
        const val EDITED_SERVICE = "사용자가 고친 제목"
        const val SERVER_SERVICE = "구글 포토"
        const val SERVER_PROCESSING_METHOD = "파일 전달"
    }
}
