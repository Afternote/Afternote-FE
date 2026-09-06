package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.api.MindRecordReceiverApiService
import com.afternote.feature.mindrecord.data.dto.ReceiverDailyQuestionListDto
import com.afternote.feature.mindrecord.data.dto.ReceiverDiaryListDto
import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 수신자 조회 실패의 계층 경계 가드 (#614).
 *
 * "서버 에러 코드" 는 전송 계층 개념이라 화면까지 올라가면 안 된다. 여기서 도메인 예외로
 * 바꿔 두면 presentation 은 타입만 보고 분기하고 `core:network` 를 모른다.
 *
 * 변환 함수가 아니라 **[MindRecordReceiverRepositoryImpl.getAll] 을 통해** 본다 (#1512).
 * 함수만 직접 부르면 「어느 메서드에 변환이 붙는가」가 가드에서 빠져, 배선을 떼도 초록이다.
 */
class MindRecordReceiverRepositoryImplTest {
    private fun repository(api: MindRecordReceiverApiService) = MindRecordReceiverRepositoryImpl(api)

    private fun apiException(
        status: Int,
        code: Int,
    ) = ApiException(status = status, code = code, serverMessage = "서버 원문", fallbackMessage = "서버 원문")

    @Test
    fun `전달 조건 미충족 코드는 도메인 예외가 된다`() {
        val repository = repository(FakeMindRecordReceiverApiService(onDailyQuestions = { throw apiException(403, 2009) }))

        val result = runBlocking { repository.getAll() }

        assertTrue(result.exceptionOrNull() is DeliveryNotReadyException)
    }

    @Test
    fun `원래 예외를 cause 로 남긴다`() {
        // 진단 정보를 버리지 않는다 — 화면에 쓰지 않을 뿐이다.
        val thrown = apiException(403, 2009)
        val repository = repository(FakeMindRecordReceiverApiService(onDailyQuestions = { throw thrown }))

        val result = runBlocking { repository.getAll() }

        assertSame(thrown, result.exceptionOrNull()?.cause)
    }

    @Test
    fun `다른 서버 코드는 그대로 흘려보낸다`() {
        val thrown = apiException(500, 1004)
        val repository = repository(FakeMindRecordReceiverApiService(onDailyQuestions = { throw thrown }))

        assertSame(thrown, runBlocking { repository.getAll() }.exceptionOrNull())
    }

    @Test
    fun `서버 응답이 없는 실패는 손대지 않는다`() {
        val repository = repository(FakeMindRecordReceiverApiService(onDailyQuestions = { throw IOException("timeout") }))

        val exception = runBlocking { repository.getAll() }.exceptionOrNull()

        // 여기만 `assertSame` 이 아니다 — 코루틴의 stack-trace recovery 가 `async` 경계를 넘을 때
        // «(String) 생성자가 있는» 예외를 복사본으로 바꾼다(`IOException` 이 해당). 그래서 인스턴스
        // 동일성이 아니라 «변환되지 않았다» 는 계약 자체를 본다.
        assertTrue("변환 대상이 아닌 실패가 도메인 예외가 됐다", exception !is DeliveryNotReadyException)
        assertTrue(exception is IOException)
        assertEquals("timeout", exception?.message)
    }

    @Test
    fun `일기 쪽 실패에도 같은 변환이 걸린다`() {
        // 두 호출이 async 로 나란히 나가므로, 한쪽에만 변환이 걸리는 배선을 배제한다.
        val repository = repository(FakeMindRecordReceiverApiService(onDiaries = { throw apiException(403, 2009) }))

        assertTrue(runBlocking { repository.getAll() }.exceptionOrNull() is DeliveryNotReadyException)
    }

    @Test
    fun `성공은 그대로 통과한다`() {
        val repository = repository(FakeMindRecordReceiverApiService())

        val records = runBlocking { repository.getAll() }.getOrNull()

        assertEquals(emptyList<Any>(), records?.dailyQuestions)
        assertEquals(emptyList<Any>(), records?.diaries)
    }
}

private fun <T> success(data: T) = BaseResponse(status = 200, code = 200, message = "성공", data = data)

/** [MindRecordReceiverApiService] 가짜 — 기본은 빈 목록 성공이다 (`FakeAccountApiService` 와 같은 규칙). */
private class FakeMindRecordReceiverApiService(
    private val onDailyQuestions: () -> BaseResponse<ReceiverDailyQuestionListDto> = {
        success(ReceiverDailyQuestionListDto(dailyQuestions = emptyList()))
    },
    private val onDiaries: () -> BaseResponse<ReceiverDiaryListDto> = {
        success(ReceiverDiaryListDto(diaries = emptyList()))
    },
) : MindRecordReceiverApiService {
    override suspend fun getReceiverDailyQuestions(
        sort: String?,
        startDate: String?,
        endDate: String?,
    ): BaseResponse<ReceiverDailyQuestionListDto> = onDailyQuestions()

    override suspend fun getReceiverDiaries(
        sort: String?,
        startDate: String?,
        endDate: String?,
    ): BaseResponse<ReceiverDiaryListDto> = onDiaries()
}
