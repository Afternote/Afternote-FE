package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.mapper.toDomain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `GET /deep-thought` 파싱 계약 (#207, 실서버 응답 실측 2026-08-24).
 *
 * 목록은 `data.deepThoughts` 안에 있고 `data` 최상위에는 `tagCounts` 도 함께 온다 —
 * 배열이 곧 `data` 인 다른 목록 API 들과 형태가 다르다.
 */
class DeepThoughtContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `실서버 응답 형태가 파싱된다`() {
        val body =
            """
            { "status": 200, "code": 200, "message": "성공",
              "data": { "deepThoughts": [
                  { "deepThoughtId": 3, "title": "207 깊은생각", "content": "<p>본문</p>",
                    "isDraft": false, "category": "성장", "tags": [],
                    "createdAt": "2026.08.24 월", "updatedAt": "2026.08.24 월", "receivers": [] }
                ], "tagCounts": [] } }
            """.trimIndent()

        val item =
            json
                .decodeFromString(BaseResponse.serializer(DeepThoughtListDto.serializer()), body)
                .data!!
                .deepThoughts
                .single()
                .toDomain()

        assertEquals(3L, item.id)
        assertEquals("207 깊은생각", item.title)
        assertEquals("2026.08.24 월", item.createdAt)
    }

    @Test
    fun `한 건도 없으면 빈 목록이다`() {
        val body = """{ "status": 200, "code": 200, "data": { "deepThoughts": [], "tagCounts": [] } }"""

        val list =
            json
                .decodeFromString(BaseResponse.serializer(DeepThoughtListDto.serializer()), body)
                .data!!
                .deepThoughts

        assertEquals(emptyList<DeepThoughtItemDto>(), list)
    }
}
