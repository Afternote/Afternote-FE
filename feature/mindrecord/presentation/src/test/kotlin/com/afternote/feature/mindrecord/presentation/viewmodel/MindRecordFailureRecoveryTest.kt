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
            val viewModel = DailyQuestionListViewModel(repository = repository)
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
    fun `이미지를 올리는 중에는 임시저장도 나가지 않는다`() =
        runTest(dispatcher) {
            // 툴바 임시저장은 `enabled` 없는 clickable 이라 canSubmit 을 우회한다 — 그래서
            // 상태만으로는 부족하고 submit() 이 직접 막아야 한다 (리뷰 지적).
            val uploadGate = CompletableDeferred<Result<String>>()
            val repository = NeverSubmittingRepository()
            val viewModel =
                DailyQuestionWriteViewModel(
                    repository = repository,
                    photoUploadRepository = PhotoUploadRepository { _, _ -> uploadGate.await() },
                )
            advanceUntilIdle()

            val uploading = launch { viewModel.uploadImage("content://picked.jpg") }
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
            uploadGate.complete(Result.success("https://cdn.example.com/a.png"))
            uploading.join()
        }
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

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit> = Result.success(Unit)

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Unit> = Result.success(Unit)

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

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Unit> {
        createCalls++
        return Result.success(Unit)
    }

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: Long): Result<Unit> = Result.success(Unit)
}
