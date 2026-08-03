package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `GET /diary` 응답 파싱 계약 가드.
 *
 * 명세("Diary 조회")의 예시와 실서버 관측 키가 갈린다 — 명세는 `id`·`date`, 실서버는
 * `diaryId`·`createdAt`. 목록 항목의 필수 프로퍼티가 하나라도 비면
 * `MissingFieldException` 으로 그 달 일기가 통째로 사라지므로, 두 형태 모두 파싱되어야 한다.
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
    fun `실서버 형태 - diaryId createdAt 키로 파싱된다`() {
        val body =
            """
            {
              "status": 200, "code": 200, "message": "성공",
              "data": {
                "monthDiaryCount": 1,
                "weeklyDominantMood": "HAPPY",
                "diaries": [
                  {
                    "diaryId": 123,
                    "title": "QA diary title",
                    "content": "<p>본문</p>",
                    "createdAt": "2026.03.21 토",
                    "todayMood": "HAPPY"
                  }
                ]
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        val diary = decoded.data!!.diaries.single()
        assertEquals(123L, diary.diaryId)
        assertEquals("2026.03.21 토", diary.createdAt)
        assertEquals(TodayMoodDto.HAPPY, diary.todayMood)
    }

    @Test
    fun `명세 형태 - id date 키와 isDraft 도 같은 프로퍼티로 파싱된다`() {
        // 명세 예시가 그대로 온 경우. 종전 DTO 는 diaryId·createdAt 이 필수라 여기서 통째로 실패했다.
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": {
                "yearMonth": "2026-03",
                "monthDiaryCount": 18,
                "weeklyDominantMood": "HAPPY",
                "diaries": [
                  {
                    "id": 123,
                    "date": "2026-03-21",
                    "title": "가족과 함께한 저녁 식사",
                    "content": "...",
                    "todayMood": "SOSO",
                    "isDraft": true,
                    "createdAt": "2026-03-21T20:13:42"
                  }
                ],
                "receivers": [{ "receiverId": 1, "name": "박채연" }]
              }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        val diary = decoded.data!!.diaries.single()
        assertEquals(123L, diary.diaryId)
        // 두 키가 함께 오면 `createdAt` 이 이긴다 — 시각까지 있는 쪽이 정렬·표시에 더 정확하다.
        assertEquals("2026-03-21T20:13:42", diary.createdAt)
        assertEquals(true, diary.isDraft)
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
