package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.DailyQuestionCreatePayload
import com.afternote.feature.mindrecord.domain.model.DailyQuestionUpdatePayload
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [DailyQuestionWriteViewModel] 의 today 재조회 경로 가드 (#565).
 *
 * 계약 — `submit()` 이 `questionId` 부재로 재조회를 걸 때, 이미 입력돼 있는 답변을
 * 서버 임시저장본으로 덮지 않는다. 화면 진입 시점(답변이 빈 상태)에서는 종전대로 이어쓴다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyQuestionWriteViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `today 조회 실패 후 저장을 누르면 재조회가 입력한 답변을 덮지 않는다`() {
        // 1회차 조회 실패 → questionId null. 사용자가 답변을 입력한 뒤 저장을 누르면
        // submit() 이 재조회를 거는데, 이번엔 성공 + isDraft=true 라 resumeDraft() 가 돈다.
        var todayCalls = 0
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = {
                    todayCalls += 1
                    if (todayCalls == 1) {
                        Result.failure(IllegalStateException("네트워크 실패"))
                    } else {
                        Result.success(todayQuestion(isDraft = true))
                    }
                },
                onGetList = { Result.success(listOf(draft(content = "서버에 남아 있던 옛 임시저장본"))) },
            )
        val viewModel = DailyQuestionWriteViewModel(repository, NoopPhotoUploadRepository)

        viewModel.onAnswerChanged("사용자가 방금 입력한 답변")
        viewModel.submit()

        assertEquals(2, todayCalls)
        assertEquals("사용자가 방금 입력한 답변", viewModel.uiState.value.answer)
        // 재조회로 얻은 draftId 는 채워져야 이어쓰기(update)가 가능하다.
        assertEquals(7L, viewModel.uiState.value.draftId)
    }

    @Test
    fun `화면 진입 시 답변이 비어 있으면 임시저장본을 그대로 이어쓴다`() {
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.success(todayQuestion(isDraft = true)) },
                onGetList = { Result.success(listOf(draft(content = "이어쓸 본문"))) },
            )

        val viewModel = DailyQuestionWriteViewModel(repository, NoopPhotoUploadRepository)

        assertEquals("이어쓸 본문", viewModel.uiState.value.answer)
        assertEquals(7L, viewModel.uiState.value.draftId)
    }

    @Test
    fun `재조회가 사용자가 방금 고른 이미지도 옛 draft 이미지로 되돌리지 않는다`() {
        var todayCalls = 0
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = {
                    todayCalls += 1
                    if (todayCalls == 1) {
                        Result.failure(IllegalStateException("네트워크 실패"))
                    } else {
                        Result.success(todayQuestion(isDraft = true))
                    }
                },
                onGetList = {
                    Result.success(listOf(draft(content = "옛 본문", imageUrl = "https://cdn/old.jpg")))
                },
            )
        val viewModel =
            DailyQuestionWriteViewModel(
                repository,
                PhotoUploadRepository { _, _ -> Result.success("https://cdn/just-picked.jpg") },
            )

        viewModel.onAnswerChanged("사용자가 방금 입력한 답변")
        runBlocking { viewModel.uploadImage("content://just-picked") }
        viewModel.submit()

        assertEquals("https://cdn/just-picked.jpg", viewModel.uiState.value.imageUrl)
    }

    @Test
    fun `화면에 이미지가 없으면 draft 이미지를 이어받는다`() {
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.success(todayQuestion(isDraft = true)) },
                onGetList = {
                    Result.success(listOf(draft(content = "이어쓸 본문", imageUrl = "https://cdn/old.jpg")))
                },
            )

        val viewModel = DailyQuestionWriteViewModel(repository, NoopPhotoUploadRepository)

        assertEquals("https://cdn/old.jpg", viewModel.uiState.value.imageUrl)
    }

    @Test
    fun `today 조회가 계속 실패하면 저장은 사유를 남기고 요청을 보내지 않는다`() {
        val repository =
            FakeDailyQuestionRepository(
                onGetToday = { Result.failure(IllegalStateException("네트워크 실패")) },
                onGetList = { Result.success(emptyList()) },
            )
        val viewModel = DailyQuestionWriteViewModel(repository, NoopPhotoUploadRepository)

        viewModel.onAnswerChanged("답변")
        viewModel.submit()

        assertTrue(viewModel.uiState.value.submitState is SubmitState.Failed)
        assertEquals(0, repository.createCallCount)
    }

    private fun todayQuestion(isDraft: Boolean) =
        TodayDailyQuestion(
            questionId = 1L,
            day = 13,
            content = "오늘의 질문",
            isAnswered = false,
            isDraft = isDraft,
        )

    private fun draft(
        content: String,
        imageUrl: String? = null,
    ) = DailyQuestion(
        dailyQuestionId = 7L,
        title = "오늘의 질문",
        content = content,
        createdAt = "2026-08-13",
        imageUrl = imageUrl,
        isDraft = true,
    )
}

/** 미지정 경로 호출은 error 로 드러낸다 (core:data 의 Fake 들과 같은 규칙). */
private class FakeDailyQuestionRepository(
    private val onGetToday: () -> Result<TodayDailyQuestion>,
    private val onGetList: () -> Result<List<DailyQuestion>>,
) : DailyQuestionRepository {
    var createCallCount = 0
        private set

    override suspend fun getList(
        date: String?,
        draftOnly: Boolean?,
    ): Result<List<DailyQuestion>> = onGetList()

    override suspend fun getToday(): Result<TodayDailyQuestion> = onGetToday()

    override suspend fun create(payload: DailyQuestionCreatePayload): Result<Long> {
        createCallCount += 1
        // 서버가 돌려주는 "내 답변" 식별자 (#573).
        return Result.success(CREATED_ANSWER_ID)
    }

    override suspend fun update(
        id: Long,
        payload: DailyQuestionUpdatePayload,
    ): Result<Long> = error("update 는 이 시나리오에서 호출되면 안 됨")

    override suspend fun delete(id: Long): Result<Unit> = error("delete 는 이 시나리오에서 호출되면 안 됨")
}

private object NoopPhotoUploadRepository : PhotoUploadRepository {
    override suspend fun upload(
        uriString: String,
        directory: String,
    ): Result<String> = error("upload 는 이 시나리오에서 호출되면 안 됨")
}

private const val CREATED_ANSWER_ID = 19L
