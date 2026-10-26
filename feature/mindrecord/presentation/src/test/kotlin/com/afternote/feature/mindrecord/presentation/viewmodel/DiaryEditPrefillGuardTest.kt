package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * 수정 진입에서 **대상을 못 읽은 채 저장하는 것**을 막는다 (#2027).
 *
 * 수정 저장은 PATCH 다 — 보낸 값이 기존 기록을 덮는다. 프리필이 실패하면 화면은 빈 폼이므로,
 * 그 상태의 저장은 **사용자가 한 번도 보지 못한 원본을 자기 입력으로 갈아치운다.**
 *
 * 종전 가드는 `isEditingDraft && draftLoadError != null` 이었고, `isEditingDraft` 는
 * `route.isDraft` 일 때만 섰다. **정식 기록 수정(`isDraft = false`)은 같은 PATCH 경로를 쓰면서
 * 가드 밖에 있었다** — 임시저장 이어쓰기만 막고 정식 수정은 그대로 통과했다.
 *
 * 그래서 이 파일은 두 진입을 **같은 단언으로** 본다. 한쪽만 고치면 다른 쪽이 다시 열리는 것이
 * 이 결함의 모양이라, 갈래를 나눠 쓰지 않는다.
 *
 * Robolectric 은 `SavedStateHandle.toRoute()` 가 `android.os.Bundle` 을 지나기 때문에만 필요하다 —
 * 시간·디스패처는 [StandardTestDispatcher] 가 쥔다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiaryEditPrefillGuardTest {
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
    fun `정식 기록 수정은 프리필 실패 뒤 저장이 막힌다`() =
        runTest(dispatcher) {
            assertBlockedAfterPrefillFailure(isDraft = false)
        }

    @Test
    fun `임시저장 이어쓰기도 프리필 실패 뒤 저장이 막힌다`() =
        runTest(dispatcher) {
            assertBlockedAfterPrefillFailure(isDraft = true)
        }

    /** 정상 수정 계약은 그대로다 — 프리필이 성공하면 저장이 열리고 PATCH 가 나간다. */
    @Test
    fun `프리필이 성공한 정식 기록 수정은 종전처럼 저장된다`() =
        runTest(dispatcher) {
            val repository =
                FakeDiaryRepository(
                    initialDiaries =
                        listOf(
                            Diary(
                                diaryId = EDIT_ID,
                                title = "원본 제목",
                                content = "<p>원본 본문</p>",
                                date = "2026-09-13",
                                createdAt = "2026-09-13T09:00:00",
                                todayMood = TodayMood.SOSO,
                                isDraft = false,
                            ),
                        ),
                )
            val viewModel = viewModel(repository, isDraft = false)
            advanceUntilIdle()

            viewModel.onTitleChanged("고친 제목")
            viewModel.onContentChanged("<p>고친 본문</p>")
            viewModel.onMoodSelected(TodayMood.HAPPY)
            assertTrue("프리필이 성공했는데 저장이 막혔다", viewModel.uiState.value.canSubmit)

            viewModel.submit(isDraft = false)
            advanceUntilIdle()

            assertTrue("정상 수정이 PATCH 를 내지 않았다", repository.updatedPayloads.isNotEmpty())
        }

    /** 신규 작성은 덮어쓸 원본이 없다 — 목록 조회가 실패해도 저장을 막지 않는다. */
    @Test
    fun `신규 작성은 목록 조회 실패와 무관하게 저장된다`() =
        runTest(dispatcher) {
            val repository = FakeDiaryRepository()
            repository.onGetList = { _, _ -> Result.failure(IOException("목록 조회 실패")) }
            val viewModel = viewModel(repository, isDraft = false, recordId = null)
            advanceUntilIdle()

            viewModel.onTitleChanged("새 제목")
            viewModel.onContentChanged("<p>새 본문</p>")
            viewModel.onMoodSelected(TodayMood.HAPPY)

            assertTrue("신규 작성이 막혔다", viewModel.uiState.value.canSubmit)
        }

    private suspend fun TestScope.assertBlockedAfterPrefillFailure(isDraft: Boolean) {
        val repository = FakeDiaryRepository()
        repository.onGetList = { _, _ -> Result.failure(IOException("프리필 조회 실패")) }
        val viewModel = viewModel(repository, isDraft = isDraft)
        advanceUntilIdle()

        assertNotNull("프리필 실패가 상태에 남지 않았다", viewModel.uiState.value.draftLoadError)

        viewModel.onTitleChanged("대체 제목")
        viewModel.onContentChanged("<p>대체 본문</p>")
        viewModel.onMoodSelected(TodayMood.HAPPY)

        assertFalse("못 읽은 원본 위로 정식 등록이 열렸다", viewModel.uiState.value.canSubmit)
        assertFalse("못 읽은 원본 위로 임시저장이 열렸다", viewModel.uiState.value.canSaveDraft)

        viewModel.submit(isDraft = false)
        viewModel.submit(isDraft = true)
        advanceUntilIdle()

        assertTrue("못 읽은 원본을 덮는 PATCH 가 나갔다", repository.updatedPayloads.isEmpty())
    }

    private fun viewModel(
        repository: FakeDiaryRepository,
        isDraft: Boolean,
        recordId: Long? = EDIT_ID,
    ) = DiaryWriteViewModel(
        savedStateHandle =
            SavedStateHandle(
                mapOf(
                    "recordId" to recordId,
                    "yearMonth" to YEAR_MONTH,
                    "isDraft" to isDraft,
                ),
            ),
        repository = repository,
        photoUploadRepository =
            FakePhotoUploadRepository(
                uploadedUrl = "https://cdn.test/image.jpg",
                uploadedKey = "mindrecords/1/image.jpg",
            ),
        userRepository =
            FakeUserRepository.strict().apply {
                onReceiverListFlow = { flowOf(emptyList()) }
                onGetReceivers = { emptyList() }
            },
        draftLoader = LoadMindRecordDraftsUseCase(repository, FakeDailyQuestionRepository()),
        errorReporter = RecordingErrorReporter(),
    )

    private companion object {
        const val EDIT_ID = 42L
        const val YEAR_MONTH = "2026-09"
    }
}
