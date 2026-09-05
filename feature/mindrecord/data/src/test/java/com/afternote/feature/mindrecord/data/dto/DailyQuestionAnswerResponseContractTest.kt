package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 생성·수정 응답의 `userDailyQuestionId` 계약을 고정한다 (#573).
 *
 * 종전에는 `BaseResponse<Unit>` 으로 받아 이 값을 버렸다. 그러면 저장 직후 그 레코드를
 * 가리키려고 목록을 다시 조회해 추측으로 골라야 한다.
 *
 * 아래 본문은 2026-08-23 `afternote.kro.kr` 실측 응답 그대로다.
 */
class DailyQuestionAnswerResponseContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `생성 응답에서 내 답변 식별자를 읽는다`() {
        val body =
            """
            {
              "status": 200, "code": 200, "message": "성공",
              "data": { "userDailyQuestionId": 19, "content": "응답 계약 실측",
                        "isDraft": true, "receivers": [] }
            }
            """.trimIndent()

        val decoded =
            json.decodeFromString(
                BaseResponse.serializer(DailyQuestionAnswerResponseDto.serializer()),
                body,
            )

        assertEquals(19L, decoded.data!!.userDailyQuestionId)
        assertEquals("응답 계약 실측", decoded.data!!.content)
        assertEquals(true, decoded.data!!.isDraft)
    }

    @Test
    fun `수정 응답도 같은 스키마다`() {
        val body =
            """
            {
              "status": 200, "code": 200, "message": "성공",
              "data": { "userDailyQuestionId": 19, "content": "응답 계약 실측 수정",
                        "isDraft": true, "receivers": [] }
            }
            """.trimIndent()

        val decoded =
            json.decodeFromString(
                BaseResponse.serializer(DailyQuestionAnswerResponseDto.serializer()),
                body,
            )

        assertEquals(19L, decoded.data!!.userDailyQuestionId)
    }

    @Test
    fun `식별자가 없으면 실패로 드러난다`() {
        // 기본값을 두지 않는다 — 이 값이 없으면 저장 직후 흐름이 성립하지 않으므로
        // 조용히 0 으로 메우지 않고 파싱에서 막는다.
        val body =
            """
            {
              "status": 200, "code": 200, "message": "성공",
              "data": { "content": "식별자 없음", "isDraft": true }
            }
            """.trimIndent()

        val failed =
            runCatching {
                json.decodeFromString(
                    BaseResponse.serializer(DailyQuestionAnswerResponseDto.serializer()),
                    body,
                )
            }.isFailure

        assertEquals(true, failed)
    }

    @Test
    fun `isDraft 가 빠지면 정식 답변으로 접히지 않고 실패한다`() {
        // false 로 접히면 임시저장이 정식 답변으로 보인다 — 계약 변경이 «정상적인 빈 값» 으로
        // 둔갑하는 자리라 기본값을 두지 않는다 (#789).
        val body =
            """
            {
              "status": 200, "code": 200, "message": "성공",
              "data": { "userDailyQuestionId": 19, "content": "응답 계약 실측" }
            }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(
                BaseResponse.serializer(DailyQuestionAnswerResponseDto.serializer()),
                body,
            )
        }
    }

    @Test
    fun `content 가 빠져도 빈 문자열로 성공하지 않는다`() {
        val body =
            """
            {
              "status": 200, "code": 200, "message": "성공",
              "data": { "userDailyQuestionId": 19, "isDraft": false }
            }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(
                BaseResponse.serializer(DailyQuestionAnswerResponseDto.serializer()),
                body,
            )
        }
    }
}
