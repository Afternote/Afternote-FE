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
 * Json 설정은 `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys).
 */
@OptIn(ExperimentalSerializationApi::class)
class WeeklyReportContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    // 카운트·요약·목록 키는 모두 계약이라 기본값이 없다 (#789) — 주(week) 형태만 바꿔가며
    // 보기 위해 나머지 필수 키는 여기서 채운다.
    private fun decodeWeek(weekJson: String): List<WeeklyReportDayDto> {
        val body =
            """
            { "status": 200, "code": 200, "data": {
              "dailyQuestionAmount": 0, "diaryAmount": 0, "summaryText": "",
              "daily-question": [], "emotions": [], "week": $weekJson,
              "emotionAnalysis": { "total": 1, "succeeded": 1, "pending": 0, "failed": 0 } } }
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
                [{ "diaryId": 1, "day": 1, "type": "DAILY_QUESTION", "emotion": null },
                 { "diaryId": 2, "day": 2, "type": "DEEP_THOUGHT", "emotion": null }]
                """.trimIndent(),
            )

        assertEquals(listOf(false, false), week.map { it.toDomain().isDiary })
    }

    @Test
    fun `명세에 없는 새 종류가 와도 그 주가 죽지 않고 일기 아님으로 접힌다`() {
        // type 을 enum 으로 받으면 여기서 그 주 전체가 MissingFieldException 으로 날아간다.
        val week = decodeWeek("""[{ "diaryId": 1, "day": 1, "type": "FUTURE_KIND", "emotion": null }]""")

        assertEquals(false, week.single().toDomain().isDiary)
    }

    @Test
    fun `type 키가 빠진 응답은 실패한다`() {
        // 명세가 required 로 선언한 값이라 조용히 기본값으로 성공시키지 않는다.
        assertThrows(MissingFieldException::class.java) {
            decodeWeek("""[{ "diaryId": 1, "day": 1, "emotion": null }]""")
        }
    }

    @Test
    fun `종전 isDiary 키만 온 형태는 더 이상 통과하지 않는다`() {
        // 회귀 방향 고정 — isDiary 로 되돌리면 이 테스트가 먼저 깨진다.
        assertThrows(MissingFieldException::class.java) {
            decodeWeek("""[{ "diaryId": 1, "day": 1, "isDiary": true, "emotion": null }]""")
        }
    }

    @Test
    fun `클라가 모르는 기분 값은 그 원소의 기분만 비운다`() {
        // 서버가 한글 감정을 내려준 전례가 있다 (#591). #789 로 기본값을 걷어냈어도
        // **값** 확장까지 실패로 만들지는 않는다 — 이모지 한 칸 대신 그 주가 통째로 날아간다.
        val week = decodeWeek("""[{ "diaryId": 1, "day": 1, "type": "DIARY", "emotion": "슬픔" }]""")

        val day = week.single().toDomain()
        assertTrue(day.isDiary)
        assertNull(day.emotion)
    }

    @Test
    fun `emotion 이 명시적 null 이면 정상 파싱된다`() {
        // 일기가 아닌 기록에는 기분이 없다 — 실제 의미가 있는 null 이라 nullable 은 유지한다.
        val week = decodeWeek("""[{ "diaryId": 1, "day": 1, "type": "DEEP_THOUGHT", "emotion": null }]""")

        assertNull(week.single().emotion)
    }

    @Test
    fun `emotion 키 자체가 빠지면 실패한다`() {
        // 값이 조건부인 것과 키가 사라진 것은 다르다 — 후자는 계약 변경이라 드러나야 한다 (#789).
        assertThrows(MissingFieldException::class.java) {
            decodeWeek("""[{ "diaryId": 1, "day": 1, "type": "DIARY" }]""")
        }
    }

    @Test
    fun `week 키가 없으면 빈 목록으로 접히지 않고 실패한다`() {
        // 기록 0 건은 `"week": []` 로 온다. 키가 사라진 건 계약이 바뀐 것이라,
        // 빈 목록으로 수렴시키면 "이번 주 기록 없음" 리포트와 구분되지 않는다 (#789).
        val body =
            """
            { "status": 200, "code": 200, "data": {
              "dailyQuestionAmount": 0, "diaryAmount": 0, "summaryText": "",
              "daily-question": [], "emotions": [] } }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(WeeklyReportDto.serializer()), body)
        }
    }

    @Test
    fun `카운트 키가 빠지면 0 으로 접히지 않고 실패한다`() {
        // `0` 으로 접히면 잘못된 주간 리포트가 정상 화면으로 보인다 (#789).
        val body =
            """
            { "status": 200, "code": 200, "data": {
              "dailyQuestionAmount": 0, "summaryText": "",
              "daily-question": [], "emotions": [], "week": [] } }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(WeeklyReportDto.serializer()), body)
        }
    }
}
