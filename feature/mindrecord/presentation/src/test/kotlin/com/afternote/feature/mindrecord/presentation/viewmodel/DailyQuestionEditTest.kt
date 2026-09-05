package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
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

    @Test
    fun `임시저장 이어쓰기는 draft 목록에서 대상을 찾는다`() {
        // 당일이 지난 draft 는 draftOnly=true 로만 내려온다 — 이 경로가 유일한 진입 수단이다 (#770).
        val repository =
            FakeRepository(
                answers = emptyList(),
                drafts = listOf(answer(id = 11L, content = "<p>지난 임시저장</p>")),
            )

        val viewModel = editViewModel(repository, answerId = 11L, isDraft = true)

        assertEquals("<p>지난 임시저장</p>", viewModel.uiState.value.answer)
        assertEquals(11L, viewModel.uiState.value.draftId)
        assertEquals("draft 목록을 조회해야 한다", listOf(true), repository.listDraftOnlyArgs)
    }

    @Test
    fun `임시저장 이어쓰기를 등록하면 같은 레코드를 확정으로 바꾼다`() {
        val repository =
            FakeRepository(
                answers = emptyList(),
                drafts = listOf(answer(id = 11L, content = "<p>지난 임시저장</p>")),
            )
        val viewModel = editViewModel(repository, answerId = 11L, isDraft = true)

        viewModel.submit(isDraft = false)

        assertEquals(0, repository.createCalls)
        assertEquals(listOf(11L), repository.updatedIds)
        assertEquals(false, repository.updatedPayloads.single().isDraft)
    }

    @Test
    fun `정식 수정은 draft 목록을 조회하지 않는다`() {
        val repository = FakeRepository(answers = listOf(answer(id = 7L, content = "<p>원본</p>")))

        editViewModel(repository, answerId = 7L, isDraft = false)

        assertEquals(listOf<Boolean?>(null), repository.listDraftOnlyArgs)
    }

    // ── 테스트 도구 ───────────────────────────────────────────────────────────

    private fun editViewModel(
        repository: DailyQuestionRepository,
        answerId: Long?,
        isDraft: Boolean = false,
    ): DailyQuestionWriteViewModel {
        val handle =
            SavedStateHandle(
                if (answerId == null) {
                    emptyMap()
                } else {
                    mapOf("answerId" to answerId, "isDraft" to isDraft)
                },
            )
        return DailyQuestionWriteViewModel(
            handle,
            repository,
            FakePhotoUploadRepository.strict(),
            // 툴바 카운트는 이 시나리오의 관심사가 아니다 — **다른** 저장소를 넘긴다.
            // 같은 fake 를 넘기면 카운트 조회의 draftOnly=true 가 프리필 조회 기록에 섞여
            // «어느 목록을 봤는가» 단언이 무너진다 (#769·#770).
            LoadMindRecordDraftsUseCase(
                diaryRepository = EditTestEmptyDiaryRepository,
                dailyQuestionRepository = EditTestEmptyDailyQuestionRepository,
            ),
            RecordingErrorReporter(),
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
        private val drafts: List<DailyQuestion> = emptyList(),
    ) : DailyQuestionRepository {
        var todayCalls = 0
        var createCalls = 0
        val updatedIds = mutableListOf<Long>()
        val updatedPayloads = mutableListOf<DailyQuestionUpdatePayload>()
        val listDraftOnlyArgs = mutableListOf<Boolean?>()

        override suspend fun getList(
            date: String?,
            draftOnly: Boolean?,
        ): Result<List<DailyQuestion>> {
            listDraftOnlyArgs += draftOnly
            return Result.success(if (draftOnly == true) drafts else answers)
        }

        override suspend fun getToday(): Result<TodayDailyQuestion> {
            todayCalls++
            return Result.success(
                TodayDailyQuestion(questionId = 1L, day = 1, content = "오늘의 질문", isAnswered = false),
            )
        }

        override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> {
            createCalls++
            return Result.success(1L)
        }

        override suspend fun update(
            id: Long,
            payload: DailyQuestionUpdatePayload,
        ): Result<Long> {
            updatedIds += id
            updatedPayloads += payload
            return Result.success(id)
        }

        override suspend fun delete(id: Long): Result<Unit> = error("delete 는 이 시나리오에서 호출되면 안 됨")
    }
}

/** 툴바 카운트 조회만 받아 주는 빈 일기 저장소. */
private object EditTestEmptyDiaryRepository : DiaryRepository {
    override suspend fun getList(
        yearMonth: String,
        draftOnly: Boolean?,
    ): Result<DiaryList> = Result.success(DiaryList(diaries = emptyList(), monthDiaryCount = 0, weeklyDominantMood = null))

    override suspend fun create(payload: DiaryCreatePayload): Result<Unit> = error("호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: DiaryUpdatePayload,
    ): Result<Unit> = error("호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}

/** 툴바 카운트 조회만 받아 주는 빈 데일리질문 저장소. */
private object EditTestEmptyDailyQuestionRepository : DailyQuestionRepository {
    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(emptyList())

    override suspend fun getToday(): Result<TodayDailyQuestion> = error("호출되면 안 됨")

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> = error("호출되면 안 됨")

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = error("호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}
