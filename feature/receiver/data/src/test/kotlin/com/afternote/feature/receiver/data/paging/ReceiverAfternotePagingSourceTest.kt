package com.afternote.feature.receiver.data.paging

import androidx.paging.PagingSource
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.data.reporting.RECEIVER_LIST_DECODING_STAGE
import com.afternote.feature.receiver.data.reporting.RECEIVER_LIST_MAPPING_STAGE
import com.afternote.feature.receiver.data.reporting.RecordingErrorReporter
import com.afternote.feature.receiver.data.reporting.assertReceiverListFailureContract
import com.afternote.feature.receiver.data.service.ReceiverAfternoteApiService
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 목록 로드 실패의 도메인 어휘 변환 회귀 가드 (#611).
 *
 * 이 경로는 형제인 `ReceiverAuthRepositoryImpl` 과 달리 번역이 없어 `ApiException`(인프라 타입)을
 * 도메인 밖으로 그대로 흘렸다. 그 결과 화면이 사유를 가를 수 없어, 재시도로 풀리지 않는 «전달 조건
 * 미충족»(403 / BE `ErrorCode.DELIVERY_CONDITION_NOT_MET` = 2009) 까지 "다시 시도" 로 수렴했다.
 *
 * code·문구는 2026-07-30 실기기 logcat 캡처 —
 * `<-- 403` `{"status":403,"code":2009,"message":"아직 전달 조건이 충족되지 않았습니다."}`.
 */
class ReceiverAfternotePagingSourceTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    /**
     * 사유를 아는 거절은 **타입** 으로 나온다 — 소비처가 `serverCode == 2009` 를 되짚지 않아도 되게,
     * 서버 code 지식을 이 계층에 가둔 결과다.
     */
    @Test
    fun `전달 조건 미충족 403 은 전용 도메인 타입으로 나온다`() {
        val result = loadWith { throw DELIVERY_CONDITION_NOT_MET_EXCEPTION }

        val error = (result as PagingSource.LoadResult.Error).throwable
        assertTrue("전용 타입으로 번역돼야 한다: $error", error is ReceiverFailure.DeliveryConditionNotMet)
        assertEquals(DELIVERY_CONDITION_NOT_MET_EXCEPTION, error.cause)
    }

    @Test
    fun `표시 사유를 아는 서버 거절은 code 대신 도메인 사유를 싣는다`() {
        val otherRejection =
            ApiException(
                status = 400,
                code = 1902,
                serverMessage = "인증번호가 만료되었습니다.",
                fallbackMessage = "만료",
            )

        val result = loadWith { throw otherRejection }

        val error = (result as PagingSource.LoadResult.Error).throwable
        assertTrue("UserRejection 이어야 한다: $error", error is ReceiverFailure.UserRejection)
        val rejection = error as ReceiverFailure.UserRejection
        assertEquals(ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND, rejection.reason)
        assertEquals(otherRejection, rejection.cause)
    }

    @Test
    fun `서버 거절은 연결 실패로 뭉개지지 않는다`() {
        val result = loadWith { throw DELIVERY_CONDITION_NOT_MET_EXCEPTION }

        val error = (result as PagingSource.LoadResult.Error).throwable
        assertTrue("연결 실패로 뭉개졌다: $error", error is ReceiverFailure.DeliveryConditionNotMet)
    }

    @Test
    fun `서버에 닿지 못한 실패는 연결 없음으로 옮기고 원인을 보존한다`() {
        val offline = IOException("Unable to resolve host")

        val result = loadWith { throw offline }

        val error = (result as PagingSource.LoadResult.Error).throwable
        assertTrue("연결 실패로 번역돼야 한다: $error", error is ReceiverFailure.NetworkUnavailable)
        assertEquals(offline, error.cause)
    }

    /** 사유를 확인하지 못한 실패는 감싸지 않는다 — 없는 status·code 를 지어내지 않기 위해서다. */
    @Test
    fun `분류 대상이 아닌 실패는 원본 그대로 흘려보낸다`() {
        val unexpected = IllegalStateException("boom")

        val result = loadWith { throw unexpected }

        assertEquals(unexpected, (result as PagingSource.LoadResult.Error).throwable)
    }

    @Test
    fun `정상 응답은 단일 페이지로 실린다`() {
        val result =
            loadWith {
                BaseResponse(
                    status = 200,
                    code = 200,
                    data =
                        ReceivedAfternoteListDto(
                            afternotes = listOf(ReceivedAfternoteDto(id = 5L, title = "소셜 계정 정리 부탁해", category = "SOCIAL")),
                            totalCount = 1,
                        ),
                )
            }

        val page = result as PagingSource.LoadResult.Page
        assertEquals(1, page.data.size)
        assertEquals(5L, page.data.first().id)
    }

    @Test
    fun `raw 응답에서 디코딩 불가와 미지원 category 항목만 제외하고 유효 항목은 페이지로 반환한다`() {
        val reporter = RecordingErrorReporter()
        val result =
            loadWith(reporter) {
                json.decodeFromString<BaseResponse<ReceivedAfternoteListDto>>(
                    """
                    {
                      "status": 200,
                      "code": 200,
                      "data": {
                        "afternotes": [
                          {"id":5,"title":"소셜 계정","category":"SOCIAL"},
                          {
                            "id":987654321,
                            "title":"sensitive-title-marker",
                            "category":"SENSITIVE_CATEGORY_MARKER"
                          },
                          {"id":876543210,"title":"missing-category-title-marker"},
                          {"id":765432109,"title":"null-category-title-marker","category":null},
                          {"id":8,"title":"사업자 항목","category":"BUSINESS"}
                        ],
                        "totalCount": 9
                      }
                    }
                    """.trimIndent(),
                )
            }

        assertTrue("일부 잘못된 항목 때문에 페이지 전체가 실패했다: $result", result is PagingSource.LoadResult.Page)
        val page = result as PagingSource.LoadResult.Page
        assertEquals(listOf(5L, 8L), page.data.map { it.id })

        reporter.assertReceiverListFailureContract(
            mapOf(
                RECEIVER_LIST_DECODING_STAGE to "2",
                RECEIVER_LIST_MAPPING_STAGE to "1",
            ),
        )

        val reportedPayload =
            reporter.failures.joinToString { failure ->
                failure.throwable.message.orEmpty() + failure.attributes.toString()
            }
        assertTrue("raw category 가 보고됐다: $reportedPayload", "SENSITIVE_CATEGORY_MARKER" !in reportedPayload)
        assertTrue("raw id 가 보고됐다: $reportedPayload", "987654321" !in reportedPayload)
        assertTrue("raw title 이 보고됐다: $reportedPayload", "sensitive-title-marker" !in reportedPayload)
        assertTrue("raw id 가 보고됐다: $reportedPayload", "876543210" !in reportedPayload)
        assertTrue("raw title 이 보고됐다: $reportedPayload", "missing-category-title-marker" !in reportedPayload)
        assertTrue("raw id 가 보고됐다: $reportedPayload", "765432109" !in reportedPayload)
        assertTrue("raw title 이 보고됐다: $reportedPayload", "null-category-title-marker" !in reportedPayload)
    }

    /**
     * 번역 경계가 취소까지 삼키면 취소된 코루틴에서 호출부의 실패 갈래가 돈다 (#671 과 같은 규약).
     *
     * 여기서는 [loadWith] 를 쓰지 않는다 — 그 헬퍼는 자체 `runBlocking` 이벤트 루프를 열어
     * 바깥 [kotlinx.coroutines.Job.cancel] 이 안쪽 대기에 닿지 못한다(테스트가 멈춘다).
     */
    @Test
    fun `취소는 실패로 바뀌지 않고 그대로 전파된다`() =
        runBlocking {
            val source =
                ReceiverAfternotePagingSource(
                    api = FakeReceiverAfternoteApiService { awaitCancellation() },
                    errorReporter = RecordingErrorReporter(),
                )
            var observed: Throwable? = null
            val job =
                launch {
                    runCatching {
                        source.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 50, placeholdersEnabled = false))
                    }.onFailure { observed = it }
                }
            yield()
            job.cancel()
            job.join()

            assertTrue("취소가 Result 로 삼켜졌다: $observed", observed == null || observed is CancellationException)
        }

    private fun loadWith(
        errorReporter: ErrorReporter = RecordingErrorReporter(),
        response: suspend () -> BaseResponse<ReceivedAfternoteListDto>,
    ): PagingSource.LoadResult<Int, AfterNoteListItem> =
        runBlocking {
            ReceiverAfternotePagingSource(FakeReceiverAfternoteApiService(response), errorReporter)
                .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 50, placeholdersEnabled = false))
        }

    private companion object {
        val DELIVERY_CONDITION_NOT_MET_EXCEPTION =
            ApiException(
                status = 403,
                code = 2009,
                serverMessage = "아직 전달 조건이 충족되지 않았습니다.",
                fallbackMessage = "아직 전달 조건이 충족되지 않았습니다.",
            )
    }
}

private class FakeReceiverAfternoteApiService(
    private val response: suspend () -> BaseResponse<ReceivedAfternoteListDto>,
) : ReceiverAfternoteApiService {
    override suspend fun getReceiverAfternotes(): BaseResponse<ReceivedAfternoteListDto> = response()

    override suspend fun getReceiverAfternoteDetail(afternoteId: Long) = error("이 테스트에서 호출되지 않는다")
}
