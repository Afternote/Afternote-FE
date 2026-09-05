package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.model.DiaryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

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
                questionId = 1L,
                isQuestionLoading = false,
                isResumingDraft = true,
            )

        assertEquals(false, state.canSubmit)
    }

    @Test
    fun `불러오기가 끝나면 저장이 다시 열린다`() {
        // 차단만 걸고 복귀를 빠뜨리면 저장이 영구히 잠긴다.
        // #582 로 canSubmit 이 «questionId 또는 draftId 중 하나» 도 요구한다 — 수정 모드는
        // 오늘 질문을 부르지 않아 questionId 가 없기 때문이다. 신규 작성 상태를 재현한다.
        val state =
            DailyQuestionWriteUiState(
                answer = "무언가",
                questionId = 1L,
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
                savedStateHandle = SavedStateHandle(emptyMap()),
                repository = repository,
                photoUploadRepository = FakePhotoUploadRepository(uploadedUrl = "", uploadedKey = ""),
                // 툴바 카운트는 이 테스트의 관심사가 아니다 — 같은 저장소를 넘겨 0건으로 둔다 (#769).
                draftLoader =
                    LoadMindRecordDraftsUseCase(
                        diaryRepository = NoDiaryDraftsRepository,
                        dailyQuestionRepository = repository,
                    ),
                errorReporter = RecordingErrorReporter(),
            )
        viewModel.onAnswerChanged("<p></p>")
        repository.releaseDraft()

        assertEquals(true, repository.lastDraftOnly)
    }

    @Test
    fun `이어쓰기 조회 실패는 «임시저장 없음» 과 구분해 알린다`() {
        // 저장이 upsert 다 — 실패를 삼키면 사용자가 빈 화면을 «아직 임시저장이 없다» 로 읽고,
        // 그대로 저장하는 순간 서버에 남아 있던 임시저장이 덮인다 (#1018).
        val viewModel = viewModel(currentAnswerFromEditor = "<p></p>", listFailure = IOException("offline"))

        val state = viewModel.uiState.value
        assertEquals(
            UiText.Resource(R.string.mindrecord_error_daily_question_draft_load_failed),
            state.draftResumeError,
        )
        assertEquals(false, state.isResumingDraft)
        // 실패했으므로 이어쓸 것을 찾지 못한 상태다 — 없는 draftId 를 지어내지 않는다.
        assertEquals(null, state.draftId)
    }

    @Test
    fun `이어쓰기 조회가 실패한 동안에는 저장할 수 없다`() {
        // 경고만 띄우고 저장을 열어 두면 아무것도 막지 못한다. 빈 에디터가 내보내는
        // `<p></p>` 도 isNotBlank() 라 버튼이 살아 있고, draftId 가 null 인 채 POST 로 나가
        // 서버 upsert 가 기존 임시저장을 빈 본문으로 덮는다 (#1018 리뷰).
        val viewModel = viewModel(currentAnswerFromEditor = "<p></p>", listFailure = IOException("offline"))

        assertEquals(false, viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `실패 상태에서 저장을 눌러도 서버로 나가지 않는다`() {
        // canSubmit 만 보면 화면 버튼은 막혀도 submit() 이 직접 불릴 때가 남는다.
        val repository = fakeRepository(listFailure = IOException("offline"))
        val viewModel = viewModelWith(repository, currentAnswerFromEditor = "<p>사용자가 쓴 답변</p>")

        viewModel.submit(isDraft = true)
        viewModel.submit(isDraft = false)

        assertEquals(0, repository.createCalls)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun `재시도가 성공하면 안내가 걷히고 저장이 다시 열린다`() {
        // 차단만 걸고 푸는 길이 없으면 사용자가 쓴 답변을 들고 갇힌다.
        val repository = fakeRepository(listFailure = IOException("offline"))
        val viewModel = viewModelWith(repository, currentAnswerFromEditor = "<p>사용자가 쓴 답변</p>")
        assertNotNull(viewModel.uiState.value.draftResumeError)

        repository.clearListFailure()
        viewModel.retryResumeDraft()

        assertEquals(null, viewModel.uiState.value.draftResumeError)
        assertEquals(true, viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `빈 목록도 이어쓰기 실패로 차단한다`() {
        // 이 경로는 today.isDraft=true 일 때만 돈다 — 서버가 «임시저장이 있다» 고 이미 말한
        // 상태다. 그런데 /today 는 서버의 LocalDate.now() 로 고르고 이 목록은 **기기의**
        // LocalDate.now() 를 필터로 보내므로, 날짜 경계·시간대가 어긋나면 실제 draft 가
        // 있어도 200 빈 목록이 온다. 그때 잠금을 풀면 POST 가 그 레코드를 upsert 해 사용자가
        // 보지 못한 본문을 덮는다 (#1018 리뷰).
        val repository = fakeRepository(drafts = emptyList())
        val viewModel = viewModelWith(repository, currentAnswerFromEditor = "<p>사용자가 쓴 답변</p>")

        val state = viewModel.uiState.value
        assertEquals(
            UiText.Resource(R.string.mindrecord_error_daily_question_draft_load_failed),
            state.draftResumeError,
        )
        assertEquals(false, state.canSubmit)

        viewModel.submit(isDraft = true)
        viewModel.submit(isDraft = false)
        assertEquals(0, repository.createCalls)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun `정상 경로에서도 빈 HTML 은 저장할 수 없다`() {
        // 이어쓰기 차단은 «불러오지 못한» 경우만 막는다. 임시저장이 아예 없어 정상적으로
        // 신규 작성으로 들어온 화면에서 에디터를 비우면 `<p></p>` 가 남는데, isNotBlank() 는
        // 이것을 «썼다» 로 읽어 버튼이 살아 있었다 — 빈 답변이 그대로 create 됐다 (#1018 리뷰).
        val repository = fakeRepository(todayIsDraft = false)
        val viewModel = viewModelWith(repository, currentAnswerFromEditor = "<p></p>")

        assertEquals(false, viewModel.uiState.value.canSubmit)

        // 하단 툴바 임시저장은 `enabled` 없는 clickable 이라 canSubmit 을 우회한다.
        viewModel.submit(isDraft = true)
        viewModel.submit(isDraft = false)
        assertEquals(0, repository.createCalls)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun `이어쓴 본문을 지우면 기존 임시저장을 빈 값으로 덮지 않는다`() {
        // 조회는 성공해 draftId 가 잡혀 있다 — 이 상태에서 저장이 열리면 PATCH 가 나가
        // 서버의 기존 본문이 빈 HTML 로 덮인다. 되돌릴 수 없는 유실이다.
        val repository = fakeRepository()
        val viewModel = viewModelWith(repository, currentAnswerFromEditor = "<p></p>")
        assertEquals("<p>이어쓸 본문</p>", viewModel.uiState.value.answer)
        assertEquals(7L, viewModel.uiState.value.draftId)

        viewModel.onAnswerChanged("<p></p>")

        assertEquals(false, viewModel.uiState.value.canSubmit)
        viewModel.submit(isDraft = true)
        viewModel.submit(isDraft = false)
        assertEquals(0, repository.updateCalls)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `사진만 있고 글자가 없는 본문은 저장할 수 있다`() {
        // 태그를 통째로 걷어 판정하면 «이미지 한 장» 이 빈 것으로 접혀 저장이 막힌다.
        // 사진만 남기는 기록은 정상 입력이다.
        val repository = fakeRepository(todayIsDraft = false)
        val viewModel =
            viewModelWith(repository, currentAnswerFromEditor = """<p><img src="https://cdn.example.com/a.png" /></p>""")

        assertEquals(true, viewModel.uiState.value.canSubmit)
    }

    private fun fakeRepository(
        listFailure: Throwable? = null,
        drafts: List<DailyQuestion>? = null,
        todayIsDraft: Boolean = true,
    ): FakeResumeRepository =
        FakeResumeRepository(
            listFailure = listFailure,
            drafts = drafts,
            today =
                TodayDailyQuestion(
                    questionId = 1L,
                    day = 16,
                    content = "질문",
                    isAnswered = false,
                    isDraft = todayIsDraft,
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

    private fun viewModel(
        currentAnswerFromEditor: String,
        listFailure: Throwable? = null,
        drafts: List<DailyQuestion>? = null,
    ): DailyQuestionWriteViewModel = viewModelWith(fakeRepository(listFailure, drafts), currentAnswerFromEditor)

    private fun viewModelWith(
        repository: FakeResumeRepository,
        currentAnswerFromEditor: String,
    ): DailyQuestionWriteViewModel {
        val viewModel =
            DailyQuestionWriteViewModel(
                savedStateHandle = SavedStateHandle(emptyMap()),
                repository = repository,
                photoUploadRepository = FakePhotoUploadRepository(uploadedUrl = "", uploadedKey = ""),
                // 툴바 카운트는 이 테스트의 관심사가 아니다 — 같은 저장소를 넘겨 0건으로 둔다 (#769).
                draftLoader =
                    LoadMindRecordDraftsUseCase(
                        diaryRepository = NoDiaryDraftsRepository,
                        dailyQuestionRepository = repository,
                    ),
                errorReporter = RecordingErrorReporter(),
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
    private val listFailure: Throwable? = null,
    private val drafts: List<DailyQuestion>? = null,
) : DailyQuestionRepository {
    private val todayArrived = CompletableDeferred<Unit>()

    fun releaseDraft() {
        todayArrived.complete(Unit)
    }

    private var failure: Throwable? = listFailure

    fun clearListFailure() {
        failure = null
    }

    var createCalls = 0
        private set
    var updateCalls = 0
        private set

    /** 마지막으로 받은 `draftOnly` — 인자를 흘리지 않는지 테스트가 본다. */
    var lastDraftOnly: Boolean? = null
        private set

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> {
        lastDraftOnly = draftOnly
        failure?.let { return Result.failure(it) }
        return Result.success(drafts ?: listOf(draft))
    }

    override suspend fun getToday(): Result<TodayDailyQuestion> {
        todayArrived.await()
        return Result.success(today)
    }

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> {
        createCalls += 1
        return Result.success(1L)
    }

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> {
        updateCalls += 1
        return Result.success(1L)
    }

    override suspend fun delete(id: Long): Result<Unit> = error("호출되면 안 됨")
}

/** 툴바 카운트 조회만 받아 주는 빈 일기 저장소. 같은 패키지에 동명 fake 가 있어 이름을 달리한다. */
private object NoDiaryDraftsRepository : DiaryRepository {
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
