package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.mindrecord.data.mapper.toDomain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `GET /mind-record/weekly` 응답 파싱 계약 가드 (OpenAPI `WeekRecordItem` 실측, 2026-08-15).
 *
 * ```
 * WeekRecordItem  required: [day, diaryId, type]
 *   type  string  enum=[DIARY, DAILY_QUESTION, DEEP_THOUGHT]
 * ```
 *
 * 기록 종류는 `isDiary` 불리언이 아니라 `type` 문자열이다. 종전 DTO 는 없는 키를 읽어
 * 항상 `false` 가 됐고, 그래서 모든 일기가 캘린더 점과 기록일수에서 빠졌다 — 이 파일이
 * 그 회귀를 막는다.
 *
 * Json 설정은 `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys + coerceInputValues).
 */
@OptIn(ExperimentalSerializationApi::class)
class WeeklyReportContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    private fun decodeWeek(weekJson: String): List<WeeklyReportDayDto> {
        val body =
            """
            { "status": 200, "code": 200, "data": { "week": $weekJson } }
            """.trimIndent()
        return json
            .decodeFromString(BaseResponse.serializer(WeeklyReportDto.serializer()), body)
            .data!!
            .week
    }

    @Test
    fun `현재 와이어 형태의 DIARY 원소가 일기로 접힌다`() {
        val week = decodeWeek("""[{ "diaryId": 13, "day": 3, "type": "DIARY", "emotion": "SAD" }]""")

        val day = week.single().toDomain()
        assertEquals(13L, day.diaryId)
        assertEquals(3, day.day)
        assertTrue("type=DIARY 는 일기다 — 여기서 false 면 캘린더 점이 통째로 사라진다", day.isDiary)
    }

    @Test
    fun `DIARY 가 아닌 종류는 일기가 아니다`() {
        val week =
            decodeWeek(
                """
                [{ "diaryId": 1, "day": 1, "type": "DAILY_QUESTION" },
                 { "diaryId": 2, "day": 2, "type": "DEEP_THOUGHT" }]
                """.trimIndent(),
            )

        assertEquals(listOf(false, false), week.map { it.toDomain().isDiary })
    }

    @Test
    fun `명세에 없는 새 종류가 와도 그 주가 죽지 않고 일기 아님으로 접힌다`() {
        // type 을 enum 으로 받으면 여기서 그 주 전체가 MissingFieldException 으로 날아간다.
        val week = decodeWeek("""[{ "diaryId": 1, "day": 1, "type": "FUTURE_KIND" }]""")

        assertEquals(false, week.single().toDomain().isDiary)
    }

    @Test
    fun `type 키가 빠진 응답은 실패한다`() {
        // 명세가 required 로 선언한 값이라 조용히 기본값으로 성공시키지 않는다.
        assertThrows(MissingFieldException::class.java) {
            decodeWeek("""[{ "diaryId": 1, "day": 1 }]""")
        }
    }

    @Test
    fun `종전 isDiary 키만 온 형태는 더 이상 통과하지 않는다`() {
        // 회귀 방향 고정 — isDiary 로 되돌리면 이 테스트가 먼저 깨진다.
        assertThrows(MissingFieldException::class.java) {
            decodeWeek("""[{ "diaryId": 1, "day": 1, "isDiary": true }]""")
        }
    }

    @Test
    fun `클라가 모르는 기분 값은 그 원소의 기분만 비운다`() {
        // 서버가 한글 감정을 내려주는 중이다 (#591).
        val week = decodeWeek("""[{ "diaryId": 1, "day": 1, "type": "DIARY", "emotion": "슬픔" }]""")

        val day = week.single().toDomain()
        assertTrue(day.isDiary)
        assertNull(day.emotion)
    }

    @Test
    fun `week 키가 없으면 빈 목록으로 접힌다`() {
        val body = """{ "status": 200, "code": 200, "data": { "summaryText": "" } }"""

        val decoded = json.decodeFromString(BaseResponse.serializer(WeeklyReportDto.serializer()), body)

        assertEquals(emptyList<WeeklyReportDayDto>(), decoded.data!!.week)
    }
}
