package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
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
        val state = DailyQuestionWriteUiState(answer = "무언가", isResumingDraft = true)

        assertEquals(false, state.canSubmit)
    }

    private fun viewModel(currentAnswerFromEditor: String): DailyQuestionWriteViewModel {
        val repository =
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
        val viewModel =
            DailyQuestionWriteViewModel(
                repository = repository,
                photoUploadRepository = PhotoUploadRepository { _, _ -> Result.success("") },
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

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(listOf(draft))

    override suspend fun getToday(): Result<TodayDailyQuestion> {
        todayArrived.await()
        return Result.success(today)
    }

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit> = Result.success(Unit)

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}
