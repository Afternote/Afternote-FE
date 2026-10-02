package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.model.UploadedFile
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
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 실패 안내가 **사라지는지** 까지 본다 (#716 리뷰 지적).
 *
 * 드러내는 것만 고치고 걷는 것을 빠뜨리면, 삭제가 나중에 성공해도 «항목은 사라졌는데
 * 실패 안내는 그대로» 인 화면이 ViewModel 수명 내내 남는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MindRecordFailureRecoveryTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // runTest 와 **같은 스케줄러**를 Main 에 꽂아야 advanceUntilIdle 이 viewModelScope 의
        // 코루틴까지 굴린다. 별도 디스패처를 쓰면 조용히 어긋난다.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `삭제 실패 안내는 다음 삭제가 성공하면 사라진다`() =
        runTest(dispatcher) {
            val repository = FlakyDeleteRepository()
            val viewModel =
                DailyQuestionListViewModel(
                    repository = repository,
                    changeTracker = MindRecordChangeTracker(),
                    errorReporter = RecordingErrorReporter(),
                )
            // uiState 는 WhileSubscribed 라 구독자가 없으면 Loading 에 머문다.
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.delete(1L)
            advanceUntilIdle()
            assertNotNull(
                "실패는 드러나야 한다",
                (viewModel.uiState.value as DailyQuestionListUiState.Success).deleteError,
            )

            repository.succeedsNext = true
            viewModel.delete(1L)
            advanceUntilIdle()

            assertNull(
                "성공하면 지난 실패 문구도 걷는다",
                (viewModel.uiState.value as DailyQuestionListUiState.Success).deleteError,
            )
        }

    @Test
    fun `목록을 새로 받아 오면 옛 삭제 실패 안내도 걷힌다`() =
        runTest(dispatcher) {
            // 리뷰가 스크린샷으로 실증한 경로 — 삭제 재시도가 아니라 **재조회 성공**이다.
            // 기내모드 해제 → 탭 전환 → 목록 정상 재로드인데도 배너가 남아 있었다.
            val repository = FlakyDeleteRepository()
            val viewModel =
                DailyQuestionListViewModel(
                    repository = repository,
                    changeTracker = MindRecordChangeTracker(),
                    errorReporter = RecordingErrorReporter(),
                )
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.delete(1L)
            advanceUntilIdle()
            assertNotNull(
                "실패는 드러나야 한다",
                (viewModel.uiState.value as DailyQuestionListUiState.Success).deleteError,
            )

            viewModel.retry()
            advanceUntilIdle()

            assertNull(
                "새로 받아 왔으면 옛 실패 문구는 남지 않는다",
                (viewModel.uiState.value as DailyQuestionListUiState.Success).deleteError,
            )
        }

    @Test
    fun `이미지를 올리는 중에는 임시저장도 나가지 않는다`() =
        runTest(dispatcher) {
            // 툴바 임시저장은 `enabled` 없는 clickable 이라 canSubmit 을 우회한다 — 그래서
            // 상태만으로는 부족하고 submit() 이 직접 막아야 한다 (리뷰 지적).
            val uploadGate = CompletableDeferred<Result<UploadedFile>>()
            val repository = NeverSubmittingRepository()
            val viewModel =
                DailyQuestionWriteViewModel(
                    savedStateHandle = SavedStateHandle(emptyMap()),
                    repository = repository,
                    photoUploadRepository = FakePhotoUploadRepository(onUpload = { _, _ -> uploadGate.await() }),
                    // 툴바 카운트는 이 테스트의 관심사가 아니다 — 빈 목록으로 고정한다 (#769).
                    draftLoader =
                        LoadMindRecordDraftsUseCase(
                            diaryRepository = EmptyDiaryRepository,
                            dailyQuestionRepository = repository,
                        ),
                    errorReporter = RecordingErrorReporter(),
                )
            advanceUntilIdle()

            val uploading = launch { viewModel.uploadMedia("content://picked.jpg") }
            advanceUntilIdle()
            viewModel.onAnswerChanged("<p>답변</p>")

            viewModel.submit(isDraft = true)
            advanceUntilIdle()

            assertEquals("업로드 중에는 요청이 나가지 않는다", 0, repository.createCalls)
            assertTrue(
                "왜 막혔는지 알린다",
                viewModel.uiState.value.submitState is SubmitState.Failed,
            )

            // 업로드를 끝내 코루틴을 정리한다.
            uploadGate.complete(
                Result.success(
                    UploadedFile(
                        fileUrl = "https://cdn.example.com/a.png",
                        fileKey = "mindrecords/staging/13/a.png",
                    ),
                ),
            )
            uploading.join()
        }

    @Test
    fun `today 만 실패한 로드는 복귀 재조회를 막지 않는다`() =
        runTest(dispatcher) {
            // today 는 실패해도 화면을 막지 않는다 — 배너만 빠진다. 그래서 실패를 삼키고
            // «본 버전» 까지 찍어 두면, 복귀할 때마다 재조회를 건너뛰어 **서버가 회복돼도
            // 배너가 돌아오지 않는다**. 그 고정 상태를 본다 (#736 리뷰).
            val repository = FlakyTodayRepository()
            val viewModel =
                DailyQuestionListViewModel(
                    repository = repository,
                    changeTracker = MindRecordChangeTracker(),
                    errorReporter = RecordingErrorReporter(),
                )
            backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            assertEquals("진입 시 today 1회", 1, repository.todayCalls)
            assertNull(
                "today 가 실패했으니 배너는 비어 있다",
                (viewModel.uiState.value as DailyQuestionListUiState.Success).todayQuestion,
            )

            // 데이터는 그대로다 — 버전이 오르지 않으므로 «달라진 게 없으면 안 부른다» 규칙과
            // 정면으로 부딪히는 조건이다. 그래도 아직 못 받아 온 것은 다시 받아 와야 한다.
            repository.succeeds = true
            viewModel.refreshOnReturn()
            advanceUntilIdle()

            assertEquals("복귀 시 다시 부른다", 2, repository.todayCalls)
            assertNotNull(
                "서버가 회복되면 배너가 돌아온다",
                (viewModel.uiState.value as DailyQuestionListUiState.Success).todayQuestion,
            )

            // 이제는 완전히 성공했으니 #736 의 «달라진 게 없으면 안 부른다» 가 다시 걸린다.
            viewModel.refreshOnReturn()
            advanceUntilIdle()
            assertEquals("성공한 뒤에는 복귀해도 더 부르지 않는다", 2, repository.todayCalls)
        }
}

/** today 만 실패시키는 fake — 목록은 항상 성공한다. */
private class FlakyTodayRepository : DailyQuestionRepository {
    var succeeds = false
    var todayCalls = 0
        private set

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(emptyList())

    override suspend fun getToday(): Result<TodayDailyQuestion> {
        todayCalls++
        return if (succeeds) {
            Result.success(
                TodayDailyQuestion(questionId = 1L, day = 1, content = "질문", isAnswered = false, isDraft = false),
            )
        } else {
            Result.failure(IllegalStateException("today 실패"))
        }
    }

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> = Result.success(1L)

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = Result.success(1L)

    override suspend fun delete(id: Long): Result<Unit> = Result.success(Unit)
}

/** 첫 삭제는 실패하고, [succeedsNext] 를 켜면 성공한다. */
private class FlakyDeleteRepository : DailyQuestionRepository {
    var succeedsNext = false

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(emptyList())

    override suspend fun getToday(): Result<TodayDailyQuestion> =
        Result.success(
            TodayDailyQuestion(questionId = 1L, day = 1, content = "질문", isAnswered = false, isDraft = false),
        )

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> = Result.success(1L)

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = Result.success(1L)

    override suspend fun delete(id: Long): Result<Unit> =
        if (succeedsNext) Result.success(Unit) else Result.failure(IllegalStateException("삭제 실패"))
}

/** 업로드가 끝나지 않은 채 제출이 나가는지 세기 위한 fake — 제출은 세기만 한다. */
private class NeverSubmittingRepository : DailyQuestionRepository {
    var createCalls = 0
        private set

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = Result.success(emptyList())

    override suspend fun getToday(): Result<TodayDailyQuestion> =
        Result.success(
            TodayDailyQuestion(questionId = 1L, day = 1, content = "질문", isAnswered = false, isDraft = false),
        )

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> {
        createCalls++
        return Result.success(1L)
    }

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = Result.success(1L)

    override suspend fun delete(id: Long): Result<Unit> = Result.success(Unit)
}

/** 임시저장 카운트 조회만 받아 주는 빈 일기 저장소. */
private object EmptyDiaryRepository : DiaryRepository {
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
