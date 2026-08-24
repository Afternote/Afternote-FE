package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 이어쓰기가 임시저장 본문을 싣는지 고정한다 (#923).
 *
 * 빈 에디터는 `<p></p>` 를 내보낸다. `isBlank()` 로 "화면이 비었는지" 를 판정하면 이 값이
 * 비어 있지 않다고 나와 draft 본문이 실리지 않고, 그대로 저장하면 기존 내용이 빈 값으로
 * 덮인다 — 되돌릴 수 없는 유실이다.
 *
 * 판정은 Android 에 의존하지 않는 `isHtmlBlank()` 가 맡으므로 순수 JVM 에서 돈다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyQuestionResumeDraftTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `에디터가 빈 HTML 만 들고 있으면 임시저장 본문을 싣는다`() {
        val viewModel = viewModel(currentAnswerFromEditor = "<p></p>")

        val state = viewModel.uiState.value
        assertEquals("<p>이어쓸 본문</p>", state.answer)
        assertEquals(7L, state.draftId)
        assertTrue(state.draftLoaded)
    }

    @Test
    fun `사용자가 이미 쓴 내용이 있으면 덮지 않는다`() {
        val viewModel = viewModel(currentAnswerFromEditor = "<p>사용자가 방금 쓴 것</p>")

        assertEquals("<p>사용자가 방금 쓴 것</p>", viewModel.uiState.value.answer)
    }

    @Test
    fun `이어쓸 본문을 불러오는 동안에는 저장할 수 없다`() {
        // isQuestionLoading 을 명시하지 않으면 기본값(true)만으로 canSubmit 이 false 가 돼
        // isResumingDraft 를 통째로 지워도 통과하는 공허한 단언이 된다 (리뷰 지적).
        // 실기 순서상 loadTodayQuestion 이 isQuestionLoading=false 를 먼저 커밋한 뒤
        // resumeDraft 가 돌므로, 그 창을 그대로 재현한다.
        val state =
            DailyQuestionWriteUiState(
                answer = "무언가",
                isQuestionLoading = false,
                isResumingDraft = true,
            )

        assertEquals(false, state.canSubmit)
    }

    @Test
    fun `불러오기가 끝나면 저장이 다시 열린다`() {
        // 차단만 걸고 복귀를 빠뜨리면 저장이 영구히 잠긴다.
        val state =
            DailyQuestionWriteUiState(
                answer = "무언가",
                isQuestionLoading = false,
                isResumingDraft = false,
            )

        assertEquals(true, state.canSubmit)
    }

    @Test
    fun `이어쓰기가 끝나면 불러오기 상태가 풀리고 draftId 가 남는다`() {
        val viewModel = viewModel(currentAnswerFromEditor = "<p></p>")

        assertEquals(false, viewModel.uiState.value.isResumingDraft)
        assertEquals(7L, viewModel.uiState.value.draftId)
    }

    @Test
    fun `임시저장만 조회한다`() {
        // 서버는 draftOnly 없이 조회하면 임시저장을 제외한 답변만 내려준다 —
        // 인자를 흘리면 이어쓰기가 조용히 무산된다.
        val repository = fakeRepository()
        val viewModel =
            DailyQuestionWriteViewModel(
                repository = repository,
                photoUploadRepository = PhotoUploadRepository { _, _ -> Result.success("") },
                // 툴바 카운트는 이 테스트의 관심사가 아니다 — 같은 저장소를 넘겨 0건으로 둔다 (#769).
                draftLoader =
                    MindRecordDraftLoader(
                        diaryRepository = NoDiaryDraftsRepository,
                        dailyQuestionRepository = repository,
                    ),
            )
        viewModel.onAnswerChanged("<p></p>")
        repository.releaseDraft()

        assertEquals(true, repository.lastDraftOnly)
    }

    private fun fakeRepository(): FakeResumeRepository =
        FakeResumeRepository(
            today =
                TodayDailyQuestion(
                    questionId = 1L,
                    day = 16,
                    content = "질문",
                    isAnswered = false,
                    isDraft = true,
                ),
            draft =
                DailyQuestion(
                    dailyQuestionId = 7L,
                    title = "질문",
                    content = "<p>이어쓸 본문</p>",
                    createdAt = "2026.08.23 일",
                    isDraft = true,
                ),
        )

    private fun viewModel(currentAnswerFromEditor: String): DailyQuestionWriteViewModel {
        val repository = fakeRepository()
        val viewModel =
            DailyQuestionWriteViewModel(
                repository = repository,
                photoUploadRepository = PhotoUploadRepository { _, _ -> Result.success("") },
                // 툴바 카운트는 이 테스트의 관심사가 아니다 — 같은 저장소를 넘겨 0건으로 둔다 (#769).
                draftLoader =
                    MindRecordDraftLoader(
                        diaryRepository = NoDiaryDraftsRepository,
                        dailyQuestionRepository = repository,
                    ),
            )
        // 에디터가 컴포지션 직후 현재 HTML 을 되돌려 주는 것을 재현한다.
        viewModel.onAnswerChanged(currentAnswerFromEditor)
        repository.releaseDraft()
        return viewModel
    }
}

/**
 * 실제 순서를 재현한다 — 화면이 먼저 뜨고 에디터가 현재 HTML 을 되돌려 준 **뒤에** today
 * 응답이 도착해 `resumeDraft()` 가 돈다. 그래서 `getToday()` 를 붙잡아 둔다.
 */
private class FakeResumeRepository(
    private val today: TodayDailyQuestion,
    private val draft: DailyQuestion,
) : DailyQuestionRepository {
    private val todayArrived = CompletableDeferred<Unit>()

    fun releaseDraft() {
        todayArrived.complete(Unit)
    }

    /** 마지막으로 받은 `draftOnly` — 인자를 흘리지 않는지 테스트가 본다. */
    var lastDraftOnly: Boolean? = null
        private set

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> {
        lastDraftOnly = draftOnly
        return Result.success(listOf(draft))
    }

    override suspend fun getToday(): Result<TodayDailyQuestion> {
        todayArrived.await()
        return Result.success(today)
    }

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> = Result.success(1L)

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = Result.success(1L)

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}
