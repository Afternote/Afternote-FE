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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * **알려진 한계를 드러내는 가드** — 프로세스 사망 복원이 사용자의 미저장 편집을 서버 값으로 덮는다.
 *
 * 복원 경로는 두 갈래가 겹친다: `SavedStateHandle` 의 폼 스냅샷이 편집분을 되살리고, 곧이어
 * `init` 의 상세 재조회가 `pendingPrefill` 을 다시 발행해 UI 가 폼을 서버 값으로 덮어쓴다.
 * 이 테스트는 그 두 번째 발행이 **폼이 더러운지 묻지 않고** 일어난다는 사실을 고정한다.
 *
 * 이 축은 #1617 의 부분 PATCH 가 만든 것이 아니고 고치지도 않는다 — 스냅샷 복원과 프리필 재적용이
 * 함께 있던 종전부터의 동작이다. 부분 PATCH 는 오히려 이 경로를 **덜 위험하게** 만든다: 덮인 폼은
 * 서버 값과 같아져 「바뀐 것 없음」으로 아무 슬롯도 실리지 않고, 프리필이 도착하기 전 저장을
 * 시도하면 기준이 없다는 이유로 [AfternoteEditorMissingBaselineTest] 의 가드가 막는다.
 *
 * 제대로 고치려면 「복원된 편집이 있으면 프리필을 덮어쓰지 않거나 사용자에게 고를 기회를 준다」는
 * 화면 흐름 결정이 필요하다 — 편집 유실 안내·충돌 해소 UI 는 #1617 의 해야 할 일 2번(서버 거절 시
 * 화면 동작)과 같은 결의 별도 축이라 그쪽에서 다룬다.
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
    fun `복원된 편집이 있어도 상세 재조회 프리필이 다시 발행된다`() =
        runTest(dispatcher) {
            val repository =
                FakeAfternoteRepository.strict().apply {
                    onGetDetail = { Result.success(serverDetail()) }
                }
            val viewModel = viewModel(repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            runCurrent()

            // 복원 직후의 폼은 사용자가 고친 제목을 들고 있다.
            assertEquals(EDITED_SERVICE, viewModel.currentForm().selectedService)

            val prefill = viewModel.uiState.value.pendingPrefill
            assertNotNull("상세 재조회는 폼이 더러운지 묻지 않고 프리필을 발행한다", prefill)
            assertEquals(
                "그 프리필은 서버 값이라, UI 가 적용하면 사용자의 편집이 사라진다",
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
            content = DetailContent.Gallery(processingMethods = listOf("파일 전달")),
        )

    /** 프로세스 사망 전에 저장돼 있던 폼 스냅샷 — 사용자가 제목을 고쳐 둔 상태다. */
    private fun restoredSnapshot(): String =
        """
        {"type":"GALLERY_AND_FILES","selectedService":"$EDITED_SERVICE","receivers":[],
        "processingMethods":[],"memorialPlaylistSongs":[]}
        """.trimIndent().replace("\n", "")

    private fun viewModel(afternoteRepository: FakeAfternoteRepository): AfternoteEditorViewModel =
        AfternoteEditorViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        "initialType" to AfternoteType.GALLERY_AND_FILES,
                        "itemId" to EDIT_ID,
                        "editor_form_snapshot_v4" to restoredSnapshot(),
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
        const val EDITED_SERVICE = "사용자가 고친 제목"
        const val SERVER_SERVICE = "구글 포토"
    }
}
