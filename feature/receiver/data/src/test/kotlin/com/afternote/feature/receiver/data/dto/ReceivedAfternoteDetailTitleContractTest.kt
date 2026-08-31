package com.afternote.feature.receiver.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import com.afternote.feature.receiver.data.mapper.response.toDomain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 수신 상세 응답의 `title` 이 **반드시 온다**는 계약 회귀 가드 (#1177).
 *
 * BE 는 이 값을 비워 둘 수 없다 — 생성 요청이 `@NotBlank`(`AfternoteCreateRequest`), 컬럼이
 * `nullable = false`(`Afternote`), 수정 경로는 null 이면 기존 제목을 유지한다(`AfternoteService`).
 * 응답 조립부 3종(social·gallery·playlist)이 그 값을 그대로 싣고, 임시저장은 수신자 조회 쿼리가
 * `isDraft = false` 로 걸러 낸다. 그래서 제목 없는 수신 상세는 **서버에서 나올 수 없다.**
 *
 * 이 가드가 지키는 것은 그 계약이 깨졌을 때의 **실패 방향**이다. 예전처럼 `String? = null` 이면
 * 키가 빠져도 파싱이 조용히 통과하고 화면이 제목 없이 열려, 서버 결함이 «제목이 비어 보이는»
 * 정상 화면으로 위장된다. Json 설정은 `NetworkModule.provideJson` 과 동일 — 전역
 * `coerceInputValues` 를 걷어냈으므로(#1494) 기본값 유무와 무관하게 `null` 은 실패로 남는다.
 */
class ReceivedAfternoteDetailTitleContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `제목이 있는 응답 - 디코드부터 도메인 title 까지 도달`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"id":1,"category":"SOCIAL","title":"인스타그램","actions":["계정 삭제"],"senderName":"김철수","createdAt":"2026-08-26T14:30:00"}}"""

        val detail = json.decodeFromString<BaseResponse<ReceivedAfternoteDetailDto>>(payload).requireData().toDomain()

        assertEquals("인스타그램", detail.serviceName)
    }

    @Test
    fun `제목 키가 빠진 응답 - 빈 제목으로 성공하지 않고 어느 키인지 남기며 실패한다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"id":1,"category":"SOCIAL","senderName":"김철수"}}"""

        assertTrue(decodeFailureMessage(payload).contains("title"))
    }

    @Test
    fun `제목이 명시적 null 인 응답 - 기본값으로 접히지 않고 실패한다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"id":1,"category":"SOCIAL","title":null}}"""

        assertTrue(decodeFailureMessage(payload).contains("title"))
    }

    /** 실패 자체와 «어느 키 때문인지»를 함께 고정한다 — 진단 문구가 사라지면 서버 결함을 못 짚는다. */
    private fun decodeFailureMessage(payload: String): String =
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<ReceivedAfternoteDetailDto>>(payload)
        }.message.orEmpty()
}
