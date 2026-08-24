package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
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
 * 삭제 실패가 완료로 보고되지 않는지 고정한다 (#442).
 *
 * 항목별 `delete()` 는 실패를 던지지 않고 `Result.failure` 로 감싸므로, 결과를 검사하지 않으면
 * 서버가 거절한 항목이 목록에 남은 채 완료 토스트가 뜬다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DraftListDeleteTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `삭제에 실패하고 항목이 남아 있으면 완료가 아니라 실패로 알린다`() {
        val stubborn = dailyQuestion(id = 1L)
        val repository =
            FakeDailyQuestionDraftRepository(
                drafts = listOf(stubborn),
                // 서버가 거절해 항목이 그대로 남는 상황.
                deleteResult = Result.failure(IllegalStateException("서버 거절")),
                removesOnDelete = false,
            )
        val viewModel = viewModel(repository)
        val target = (viewModel.uiState.value as DraftListUiState.Success).items

        viewModel.delete(target)

        val state = viewModel.uiState.value as DraftListUiState.Success
        val outcome = state.deleteOutcome
        assertTrue("실패했는데 완료로 보고됨: $outcome", outcome is DraftDeleteOutcome.SomeFailed)
        assertEquals(1, (outcome as DraftDeleteOutcome.SomeFailed).failedItems.size)
        assertEquals(1, state.items.size)
    }

    @Test
    fun `삭제가 실패했어도 항목이 사라졌으면 완료로 본다`() {
        val repository =
            FakeDailyQuestionDraftRepository(
                drafts = listOf(dailyQuestion(id = 1L)),
                // 이미 없는 항목을 지우려다 404 — 사용자가 원한 결과는 이뤄졌다.
                deleteResult = Result.failure(IllegalStateException("404")),
                removesOnDelete = true,
            )
        val viewModel = viewModel(repository)
        val target = (viewModel.uiState.value as DraftListUiState.Success).items

        viewModel.delete(target)

        val state = viewModel.uiState.value as DraftListUiState.Success
        assertEquals(DraftDeleteOutcome.AllDeleted, state.deleteOutcome)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `모두 성공하면 완료로 알린다`() {
        val repository =
            FakeDailyQuestionDraftRepository(
                drafts = listOf(dailyQuestion(id = 1L)),
                deleteResult = Result.success(Unit),
                removesOnDelete = true,
            )
        val viewModel = viewModel(repository)
        val target = (viewModel.uiState.value as DraftListUiState.Success).items

        viewModel.delete(target)

        val state = viewModel.uiState.value as DraftListUiState.Success
        assertEquals(DraftDeleteOutcome.AllDeleted, state.deleteOutcome)
        assertTrue(state.items.isEmpty())
    }

    private fun viewModel(dailyQuestionRepository: DailyQuestionRepository): DraftListViewModel =
        DraftListViewModel(
            // #769 가 목록 조회를 loader 로 뽑아낸 뒤 생성자가 3개로 늘었다. 같은 두
            // 저장소를 넘겨 종전과 같은 조회 경로를 그대로 태운다.
            loader =
                MindRecordDraftLoader(
                    diaryRepository = EmptyDiaryDraftRepository,
                    dailyQuestionRepository = dailyQuestionRepository,
                ),
            diaryRepository = EmptyDiaryDraftRepository,
            dailyQuestionRepository = dailyQuestionRepository,
        )

    private fun dailyQuestion(id: Long) =
        DailyQuestion(
            dailyQuestionId = id,
            title = "질문",
            content = "임시저장 본문",
            createdAt = "2026-08-23T10:00:00",
            isDraft = true,
            imageUrl = null,
        )
}

private class FakeDailyQuestionDraftRepository(
    drafts: List<DailyQuestion>,
    private val deleteResult: Result<Unit>,
    private val removesOnDelete: Boolean,
) : DailyQuestionRepository {
    private val remaining = drafts.toMutableList()

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(remaining.toList())

    override suspend fun getToday(): Result<TodayDailyQuestion> = error("호출되면 안 됨")

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit> = error("호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Unit> = error("호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> {
        if (removesOnDelete) remaining.removeAll { it.dailyQuestionId == id }
        return deleteResult
    }
}

private object EmptyDiaryDraftRepository : DiaryRepository {
    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> = Result.success(DiaryList(diaries = emptyList<Diary>(), monthDiaryCount = 0, weeklyDominantMood = null))

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> = error("호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> = error("호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}
