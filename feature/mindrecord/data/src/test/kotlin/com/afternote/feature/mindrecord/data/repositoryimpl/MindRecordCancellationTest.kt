package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.api.DailyQuestionApiService
import com.afternote.feature.mindrecord.data.api.DiaryApiService
import com.afternote.feature.mindrecord.data.dto.DailyQuestionAnswerResponseDto
import com.afternote.feature.mindrecord.data.dto.DailyQuestionCreateRequestDto
import com.afternote.feature.mindrecord.data.dto.DailyQuestionListItemDto
import com.afternote.feature.mindrecord.data.dto.DailyQuestionUpdateRequestDto
import com.afternote.feature.mindrecord.data.dto.DiaryCreateRequestDto
import com.afternote.feature.mindrecord.data.dto.DiaryListDto
import com.afternote.feature.mindrecord.data.dto.DiaryUpdateRequestDto
import com.afternote.feature.mindrecord.data.dto.TodayDailyQuestionDto
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 코루틴 취소가 `Result.failure` 로 삼켜지지 않는지 고정한다 (#670).
 *
 * stdlib `runCatching` 은 `CancellationException` 까지 잡아 정상 반환으로 바꾼다. 그러면
 * 이미 취소된 코루틴에서 호출부의 `onFailure` 갈래(오류 상태 갱신·스낵바)가 실행된다.
 * `core:common` 의 `runCatchingCancellable` 은 취소만 다시 던지므로 호출 지점에서 그대로 빠져나간다.
 *
 * 판정 기준은 "반환값이 대입되지 않는 것" 이다 — 취소된 Job 은 어느 쪽이든 `isCancelled` 라
 * 그것만으로는 두 동작을 구분할 수 없다.
 */
class MindRecordCancellationTest {
    @Test
    fun `일기 조회 취소는 Result 로 돌아오지 않는다`() =
        runTest {
            val repository = DiaryRepositoryImpl(NeverReturningDiaryApi(), MindRecordChangeTracker())
            var returned: Result<*>? = null

            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    returned = repository.getList(yearMonth = "2026-08", draftOnly = null)
                }
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertNull(returned)
        }

    @Test
    fun `데일리질문 조회 취소는 Result 로 돌아오지 않는다`() =
        runTest {
            val repository = DailyQuestionRepositoryImpl(NeverReturningDailyQuestionApi(), MindRecordChangeTracker())
            var returned: Result<*>? = null

            val job =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    returned = repository.getList()
                }
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertNull(returned)
        }
}

/** 응답을 영영 주지 않는다 — 호출이 정지한 사이에 취소를 넣기 위한 fake. */
private class NeverReturningDiaryApi : DiaryApiService {
    override suspend fun getDiaries(
        yearMonth: String,
        draftOnly: Boolean?,
    ): BaseResponse<DiaryListDto> = awaitCancellation()

    override suspend fun createDiary(request: DiaryCreateRequestDto): BaseResponse<Unit> = awaitCancellation()

    override suspend fun updateDiary(
        diaryId: Long,
        request: DiaryUpdateRequestDto,
    ): BaseResponse<Unit> = awaitCancellation()

    override suspend fun deleteDiary(diaryId: Long): BaseResponse<Unit> = awaitCancellation()
}

private class NeverReturningDailyQuestionApi : DailyQuestionApiService {
    override suspend fun getDailyQuestions(
        date: String?,
        draftOnly: Boolean?,
    ): BaseResponse<List<DailyQuestionListItemDto>> = awaitCancellation()

    override suspend fun getTodayDailyQuestion(): BaseResponse<TodayDailyQuestionDto> = awaitCancellation()

    override suspend fun createDailyQuestion(request: DailyQuestionCreateRequestDto): BaseResponse<DailyQuestionAnswerResponseDto> =
        awaitCancellation()

    override suspend fun updateDailyQuestion(
        userDailyQuestionId: Long,
        request: DailyQuestionUpdateRequestDto,
    ): BaseResponse<DailyQuestionAnswerResponseDto> = awaitCancellation()

    override suspend fun deleteDailyQuestion(userDailyQuestionId: Long): BaseResponse<Unit> = awaitCancellation()
}
