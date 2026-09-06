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
 * 수신 상세 응답의 `senderName` 이 **반드시 온다**는 계약 회귀 가드 (#1515).
 *
 * BE 는 이 값을 비워 둘 수 없다 — `ReceivedService.getAfternote` 가 발신자를 못 찾으면 응답 대신
 * `USER_NOT_FOUND` 예외를 내고, 찾았다면 `User.name` 은 `@Column(nullable = false)` 다. 목록 응답도
 * 같은 경로(`ReceivedAfternoteResponse.from(receiver, senderName)`)로 채운다. nullable 이었던 이유는
 * BE 의 `@Schema` 에 `requiredMode` 가 없어 OpenAPI 상 optional 로 보였기 때문이다(#1177 의 `title` 과
 * 같은 경위).
 *
 * 이 가드가 지키는 것은 계약이 깨졌을 때의 **실패 방향**이다. `String? = null` 이면 키가 빠져도 파싱이
 * 조용히 통과해 «故 님의 애프터노트»·«님의 플레이리스트» 라는 틀린 문장이 정상 화면처럼 열린다.
 * Json 설정은 `NetworkModule.provideJson` 과 동일 — `coerceInputValues` 는 **기본값이 있어야**
 * 동작하므로 이 필드를 구해 주지 않는다.
 */
class ReceivedAfternoteDetailSenderNameContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `발신자 이름이 있는 응답 - 디코드부터 도메인 senderName 까지 도달`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"id":1,"category":"SOCIAL","title":"인스타그램","senderName":"김철수","actions":["계정 삭제"]}}"""

        val detail = json.decodeFromString<BaseResponse<ReceivedAfternoteDetailDto>>(payload).requireData().toDomain()

        assertEquals("김철수", detail.senderName)
    }

    @Test
    fun `발신자 이름 키가 빠진 응답 - 빈 이름으로 성공하지 않고 어느 키인지 남기며 실패한다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"id":1,"category":"SOCIAL","title":"인스타그램"}}"""

        assertTrue(decodeFailureMessage(payload).contains("senderName"))
    }

    @Test
    fun `발신자 이름이 명시적 null 인 응답 - coerceInputValues 가 구해 주지 않는다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"id":1,"category":"SOCIAL","title":"인스타그램","senderName":null}}"""

        assertTrue(decodeFailureMessage(payload).contains("senderName"))
    }

    /** 실패 자체와 «어느 키 때문인지»를 함께 고정한다 — 진단 문구가 사라지면 서버 결함을 못 짚는다. */
    private fun decodeFailureMessage(payload: String): String =
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<ReceivedAfternoteDetailDto>>(payload)
        }.message.orEmpty()
}
