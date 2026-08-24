package com.afternote.feature.mindrecord.data.mapper

import com.afternote.feature.mindrecord.data.dto.ReceiverDailyQuestionItemDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 수신자 데일리질문의 임시저장 방어 가드 (#956).
 *
 * 매퍼가 `isDraft` 를 파싱하지 않고 `false` 리터럴로 접고 있어서, 화면의
 * `filterNot { it.isDraft }` 가 **통과율 100% 인 무효 필터**였다. 서버가 draft 를 전달
 * 대상에서 빼기를 기대하는 것과, 빼지 않았을 때 막을 수단이 있는 것은 다른 문제다.
 *
 * 수신자 응답은 발신자와 같은 `DailyQuestionListResponse` 스키마를 재사용하므로
 * (Swagger 실측 2026-08-24: `[userDailyQuestionId, title, content, createdAt, isDraft, receivers]`)
 * 계약에 있는 값을 그대로 싣는다.
 */
class ReceiverMindRecordDraftTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun item(isDraft: Boolean) =
        json.decodeFromString<ReceiverDailyQuestionItemDto>(
            """
            {
              "userDailyQuestionId": 7,
              "title": "질문",
              "content": "답변",
              "createdAt": "2026.08.24 월",
              "isDraft": $isDraft,
              "receivers": []
            }
            """.trimIndent(),
        )

    @Test
    fun `임시저장 답변은 임시저장으로 실린다`() {
        assertTrue(item(isDraft = true).toDomain().isDraft)
    }

    @Test
    fun `정식 답변은 임시저장이 아니다`() {
        assertFalse(item(isDraft = false).toDomain().isDraft)
    }

    @Test
    fun `화면의 draft 제외 필터가 실제로 걸러낸다`() {
        // 종전에는 매퍼가 항상 false 를 넣어 이 필터가 아무것도 거르지 못했다.
        val records = listOf(item(isDraft = true), item(isDraft = false)).map { it.toDomain() }

        assertTrue(records.filterNot { it.isDraft }.size == 1)
    }
}
