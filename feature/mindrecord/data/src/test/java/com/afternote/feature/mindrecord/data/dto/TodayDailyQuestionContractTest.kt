package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * `GET /daily-questions/today` 응답 파싱 계약 가드 (#565, #789).
 *
 * 현재 release 의 성공 응답은 할당된 질문의 `day` 를 항상 채운다. 기본값으로 낮추면 계약
 * 누락이 정상 응답으로 둔갑하므로, **`day` 가 빠진 응답은 실패해야 한다.**
 *
 * `isAnswered`/`isDraft` 는 사정이 다르다 — 종전 키(`answered`/`draft`)로 잡혀 있어
 * `getToday()` 가 항상 실패했던 이력(#548)이 있어 과도기 대비로 구 키를 함께 받는다.
 *
 * Json 설정은 `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys).
 */
@OptIn(ExperimentalSerializationApi::class)
class TodayDailyQuestionContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private fun decode(body: String): TodayDailyQuestionDto =
        json
            .decodeFromString(BaseResponse.serializer(TodayDailyQuestionDto.serializer()), body)
            .data!!

    @Test
    fun `현재 release 성공 응답이 파싱된다`() {
        val dto =
            decode(
                """
                { "status": 200, "code": 200,
                  "data": { "questionId": 32, "day": 13, "content": "오늘의 질문",
                            "isAnswered": false, "isDraft": false } }
                """.trimIndent(),
            )

        assertEquals(32L, dto.questionId)
        assertEquals(13, dto.day)
        assertEquals("오늘의 질문", dto.content)
    }

    @Test
    fun `day 가 빠진 응답은 기본값으로 성공하지 않고 실패한다`() {
        assertThrows(MissingFieldException::class.java) {
            decode(
                """
                { "status": 200, "code": 200,
                  "data": { "questionId": 32, "content": "오늘의 질문" } }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `questionId 가 빠진 응답도 실패한다`() {
        assertThrows(MissingFieldException::class.java) {
            decode("""{ "status": 200, "code": 200, "data": { "day": 13, "content": "q" } }""")
        }
    }

    @Test
    fun `구 키 answered draft 도 과도기 동안 함께 받는다`() {
        // #548 회귀 — 이 두 키만 오던 시기에 getToday() 가 통째로 실패했다.
        val dto =
            decode(
                """
                { "status": 200, "code": 200,
                  "data": { "questionId": 32, "day": 13, "content": "q",
                            "answered": true, "draft": true } }
                """.trimIndent(),
            )

        assertEquals(true, dto.isAnswered)
        assertEquals(true, dto.isDraft)
    }

    @Test
    fun `isAnswered 키가 빠지면 미답변으로 접히지 않고 실패한다`() {
        // 기본값 `false` 를 두면 #548 같은 키 불일치가 다시 나도 "아직 답 안 함" 인
        // 정상 응답으로 조용히 통과한다 — 오늘의 질문 카드가 계속 미답변으로 보인다 (#789).
        assertThrows(MissingFieldException::class.java) {
            decode(
                """
                { "status": 200, "code": 200,
                  "data": { "questionId": 32, "day": 13, "content": "q", "isDraft": false } }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `isDraft 키가 빠지면 실패한다`() {
        assertThrows(MissingFieldException::class.java) {
            decode(
                """
                { "status": 200, "code": 200,
                  "data": { "questionId": 32, "day": 13, "content": "q", "isAnswered": false } }
                """.trimIndent(),
            )
        }
    }
}
