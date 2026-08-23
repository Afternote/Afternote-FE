package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 데일리질문 "수정하기" 경로 가드 (#582).
 *
 * 종전에는 목록의 수정하기가 빈 콜백이라 팝업만 닫히고 아무 일도 일어나지 않았다.
 * 편집 경로가 붙은 뒤에도 두 가지가 어긋나기 쉽다.
 *
 * - 수정 모드는 **오늘 질문을 부르지 않는다** — 대상은 이미 특정된 레코드다.
 * - 저장은 새 답변을 만드는 POST 가 아니라 그 레코드를 고치는 PATCH 여야 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DailyQuestionEditTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `수정 모드는 대상 답변을 프리필하고 오늘 질문을 부르지 않는다`() {
        val repository = FakeRepository(answers = listOf(answer(id = 7L, content = "<p>원본</p>")))

        val viewModel = editViewModel(repository, answerId = 7L)

        assertEquals("<p>원본</p>", viewModel.uiState.value.answer)
        assertEquals(7L, viewModel.uiState.value.draftId)
        assertEquals("오늘 질문은 부르지 않는다", 0, repository.todayCalls)
        assertTrue("에디터를 다시 시드해야 본문이 보인다", viewModel.uiState.value.contentLoaded)
    }

    @Test
    fun `수정 모드 저장은 새로 만들지 않고 PATCH 로 나간다`() {
        val repository = FakeRepository(answers = listOf(answer(id = 7L, content = "<p>원본</p>")))
        val viewModel = editViewModel(repository, answerId = 7L)

        viewModel.onAnswerChanged("<p>고친 본문</p>")
        viewModel.submit()

        assertEquals(0, repository.createCalls)
        assertEquals(listOf(7L), repository.updatedIds)
        assertEquals("<p>고친 본문</p>", repository.updatedPayloads.single().content)
    }

    @Test
    fun `수정 모드는 questionId 가 없어도 저장할 수 있다`() {
        // 명세의 PATCH 요청에는 questionId 가 없다 — 대상 레코드 ID 만 있으면 된다.
        val repository = FakeRepository(answers = listOf(answer(id = 7L, content = "<p>원본</p>")))
        val viewModel = editViewModel(repository, answerId = 7L)

        assertNull(viewModel.uiState.value.questionId)
        assertTrue("저장 버튼이 살아 있어야 한다", viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `신규 작성은 종전대로 오늘 질문을 부른다`() {
        val repository = FakeRepository(answers = emptyList())

        val viewModel = editViewModel(repository, answerId = null)

        assertEquals(1, repository.todayCalls)
        assertEquals("", viewModel.uiState.value.answer)
    }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    private fun editViewModel(
        repository: DailyQuestionRepository,
        answerId: Long?,
    ): DailyQuestionWriteViewModel {
        val handle =
            SavedStateHandle(
                if (answerId == null) emptyMap() else mapOf("answerId" to answerId),
            )
        return DailyQuestionWriteViewModel(
            handle,
            repository,
            PhotoUploadRepository { _, _ -> error("업로드는 이 시나리오에서 호출되면 안 됨") },
        )
    }

    private fun answer(
        id: Long,
        content: String,
    ) = DailyQuestion(
        dailyQuestionId = id,
        title = "오늘의 질문",
        content = content,
        createdAt = "2026.08.23 일",
    )

    private class FakeRepository(
        private val answers: List<DailyQuestion>,
    ) : DailyQuestionRepository {
        var todayCalls = 0
        var createCalls = 0
        val updatedIds = mutableListOf<Long>()
        val updatedPayloads = mutableListOf<DailyQuestionUpdatePayload>()

        override suspend fun getList(
            date: String?,
            draftOnly: Boolean?,
        ): Result<List<DailyQuestion>> = Result.success(answers)

        override suspend fun getToday(): Result<TodayDailyQuestion> {
            todayCalls++
            return Result.success(
                TodayDailyQuestion(questionId = 1L, day = 1, content = "오늘의 질문", isAnswered = false),
            )
        }

        override suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit> {
            createCalls++
            return Result.success(Unit)
        }

        override suspend fun update(
            id: Long,
            payload: DailyQuestionUpdatePayload,
        ): Result<Unit> {
            updatedIds += id
            updatedPayloads += payload
            return Result.success(Unit)
        }

        override suspend fun delete(id: Long): Result<Unit> = error("delete 는 이 시나리오에서 호출되면 안 됨")
    }
}
