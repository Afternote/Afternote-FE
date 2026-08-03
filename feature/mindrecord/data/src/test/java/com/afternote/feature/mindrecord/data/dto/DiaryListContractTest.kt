package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `GET /diary` 응답 파싱 계약 가드 (Swagger `DiaryResponse` 실측, 2026-08-03).
 *
 * 응답 스키마에 `required` 가 하나도 선언돼 있지 않아 어느 필드든 생략될 수 있다.
 * 목록 항목의 필수 프로퍼티가 하나라도 비면 `MissingFieldException` 으로 그 달 일기가
 * 통째로 사라지므로, 필드가 빠져도 살아남는지를 여기서 고정한다.
 *
 * `date`(사용자가 고른 일기 날짜)와 `createdAt`(레코드 생성 시각)은 **별개 필드**다.
 * 한 프로퍼티에 묶으면 서버의 키 순서에 따라 값이 뒤바뀌므로 분리 상태를 함께 고정한다.
 *
 * Json 설정은 `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys + coerceInputValues).
 */
class DiaryListContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `Swagger DiaryResponse 전체 필드가 파싱된다`() {
        // Swagger `DiaryResponse` 의 전 필드를 그대로 실은 형태.
        val body =
            """
            {
              "status": 200, "code": 200, "message": "성공",
              "data": {
                "yearMonth": "2026-03",
                "monthDiaryCount": 18,
                "weeklyDominantMood": "HAPPY",
                "diaries": [
                  {
                    "diaryId": 123,
                    "title": "가족과 함께한 저녁 식사",
                    "content": "<p>본문</p>",
                    "isDraft": false,
                    "emotion": "기쁨",
                    "todayMood": "HAPPY",
                    "date": "2026-03-21",
                    "createdAt": "2026-03-25T20:13:42",
                    "updatedAt": "2026-03-25T20:13:42",
                    "receivers": [{ "receiverId": 1, "name": "박채연" }]
                  }
                ]
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        val diary = decoded.data!!.diaries.single()
        assertEquals(123L, diary.diaryId)
        assertEquals(TodayMoodDto.HAPPY, diary.todayMood)
        assertEquals(false, diary.isDraft)
        // 두 날짜가 섞이지 않아야 한다. `date` 는 사용자가 고른 3/21, `createdAt` 은 생성 시각 3/25 —
        // 한 프로퍼티에 묶으면 캘린더가 3/25 칸에 찍힌다.
        assertEquals("2026-03-21", diary.date)
        assertEquals("2026-03-25T20:13:42", diary.createdAt)
    }

    @Test
    fun `노션 명세 형태의 id 키도 대체 키로 파싱된다`() {
        // 노션 명세("Diary 조회") 예시는 식별자를 `id` 로 적는다 — Swagger 와 갈려 있어 함께 받는다.
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "diaries": [{ "id": 123, "date": "2026-03-21",
                        "title": "t", "content": "c", "todayMood": "SOSO", "isDraft": true }] }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        val diary = decoded.data!!.diaries.single()
        assertEquals(123L, diary.diaryId)
        assertEquals("2026-03-21", diary.date)
        assertEquals(true, diary.isDraft)
    }

    @Test
    fun `date 가 없으면 null 로 남아 표시 단계가 createdAt 으로 폴백할 수 있다`() {
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "diaries": [{ "diaryId": 1, "title": "t", "content": "c",
                        "createdAt": "2026.03.21 토", "todayMood": "SAD" }] }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        val diary = decoded.data!!.diaries.single()
        assertNull(diary.date)
        assertEquals("2026.03.21 토", diary.createdAt)
    }

    @Test
    fun `클라가 모르는 기분 값이 와도 그 항목만 기분이 비고 목록은 살아남는다`() {
        // 명세 예시에 적힌 "SMILE" 은 클라 enum(HAPPY·SOSO·SAD)에 없다.
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "diaries": [{ "diaryId": 1, "title": "t", "content": "c",
                        "createdAt": "2026-03-21", "todayMood": "SMILE" }] }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        val diary = decoded.data!!.diaries.single()
        assertEquals(1L, diary.diaryId)
        assertNull(diary.todayMood)
    }

    @Test
    fun `diaries 키가 없으면 빈 목록으로 접힌다`() {
        // 0 건과 "키 자체가 없음" 이 같은 결과로 수렴하는 지점 — 회귀 시 여기부터 의심한다.
        val body = """{ "status": 200, "code": 200, "data": { "monthDiaryCount": 0 } }"""

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        assertEquals(emptyList<DiaryListItemDto>(), decoded.data!!.diaries)
    }
}
