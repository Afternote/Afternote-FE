package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.api.MindRecordReceiverApiService
import com.afternote.feature.mindrecord.data.dto.ReceiverDailyQuestionListDto
import com.afternote.feature.mindrecord.data.dto.ReceiverDiaryListDto
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 취소 전파 가드 (#670 의 수신자 경로) — 이 경계가 CancellationException 까지 Result 로 삼키면,
 * 로드 중 화면 이탈(viewModelScope 취소)마다 수신자 홈이 mind_records 를 실패 출처로 집계해
 * RECEIVER_HOME_PARTIAL_LOAD 논-페이탈 리포팅이 정상 취소로 오염된다 (PR #868 승인 리뷰 지적).
 */
class MindRecordReceiverRepositoryImplTest {
    @Test
    fun `getAll - in-flight 취소는 Result 로 삼키지 않고 CancellationException 을 전파`() =
        runBlocking {
            val repository = MindRecordReceiverRepositoryImpl(SuspendingMindRecordReceiverApiService())

            var result: Result<ReceiverMindRecords>? = null
            val job = launch { result = repository.getAll() }
            yield() // job 이 api 호출 지점(awaitCancellation)까지 진행하도록
            job.cancel()
            job.join()

            assertNull(result) // 취소가 Result 로 둔갑했다면 non-null 로 남는다
        }
}

/** 호출 즉시 취소까지 대기 — in-flight 상태를 재현한다. */
private class SuspendingMindRecordReceiverApiService : MindRecordReceiverApiService {
    override suspend fun getReceiverDailyQuestions(
        sort: String?,
        startDate: String?,
        endDate: String?,
    ): BaseResponse<ReceiverDailyQuestionListDto> = awaitCancellation()

    override suspend fun getReceiverDiaries(
        sort: String?,
        startDate: String?,
        endDate: String?,
    ): BaseResponse<ReceiverDiaryListDto> = awaitCancellation()
}
