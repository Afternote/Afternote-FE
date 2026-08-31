package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * `GET /diary` 응답 파싱 계약 가드 (Swagger `DiaryResponse` 실측, 2026-08-03).
 *
 * 서버가 항상 보내는 응답 키는 기본값으로 은폐하지 않는다 (#789). 키가 빠지면
 * `MissingFieldException` 으로 **실패해야** 계약 변경이 드러나므로, 그 실패를 여기서 고정한다.
 * 반대로 클라가 모르는 기분 **값**은 그 칸만 비우고 목록은 살린다 — 키 누락(계약 변경)과
 * 값 확장(표기 변경)은 성격이 다르다.
 *
 * `date`(사용자가 고른 일기 날짜)와 `createdAt`(레코드 생성 시각)은 **별개 필드**다.
 * 한 프로퍼티에 묶으면 서버의 키 순서에 따라 값이 뒤바뀌므로 분리 상태를 함께 고정한다.
 *
 * Json 설정은 `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys).
 */
class DiaryListContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
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
              "data": { "diaries": [{ "id": 123, "date": "2026-03-21", "createdAt": "2026.03.21 토",
                        "title": "t", "content": "c", "todayMood": "SOSO", "isDraft": true,
                        "receivers": [] }],
                        "monthDiaryCount": 1, "weeklyDominantMood": null }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        val diary = decoded.data!!.diaries.single()
        assertEquals(123L, diary.diaryId)
        assertEquals("2026-03-21", diary.date)
        assertEquals(true, diary.isDraft)
    }

    @Test
    fun `date 가 빠지면 빈 문자열로 성공하지 않고 실패한다`() {
        // 사용자가 고른 일기 날짜는 서버가 항상 채운다. 기본값으로 접히면 캘린더에서
        // 그 기록이 조용히 사라지므로, 누락은 파싱 실패로 드러나야 한다 (#789).
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "monthDiaryCount": 1, "weeklyDominantMood": null,
                        "diaries": [{ "diaryId": 1, "title": "t", "content": "c",
                        "createdAt": "2026.03.21 토", "todayMood": "SAD", "isDraft": false }] }
            }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)
        }
    }

    @Test
    fun `isDraft 가 빠지면 임시저장 아님으로 접히지 않고 실패한다`() {
        // `false` 로 접히면 임시저장 일기가 목록에 그대로 노출된다 (#789).
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "monthDiaryCount": 1, "weeklyDominantMood": null,
                        "diaries": [{ "diaryId": 1, "title": "t", "content": "c", "date": "2026-03-21",
                        "createdAt": "2026.03.21 토", "todayMood": "SAD" }] }
            }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)
        }
    }

    @Test
    fun `todayMood 가 빠지면 실패한다`() {
        // 저장 컬럼이 필수라 응답에도 항상 있다. 한글 값이 관측된 쪽은 AI 가 매기는
        // `emotion` 이지 사용자가 고르는 이 필드가 아니다 (#591, #789).
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "monthDiaryCount": 1, "weeklyDominantMood": null,
                        "diaries": [{ "diaryId": 1, "title": "t", "content": "c", "date": "2026-03-21",
                        "createdAt": "2026-03-21", "isDraft": false }] }
            }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)
        }
    }

    @Test
    fun `그 주에 기록이 없으면 오는 명시적 null 최빈 기분은 정상 파싱된다`() {
        // 실제로 `null` 이 오는 필드라 nullable 은 유지한다 — 다만 키 자체는 계약이다 (#789).
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "monthDiaryCount": 0, "weeklyDominantMood": null, "diaries": [] }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        assertNull(decoded.data!!.weeklyDominantMood)
        assertEquals(emptyList<DiaryListItemDto>(), decoded.data!!.diaries)
    }

    @Test
    fun `클라가 모르는 최빈 기분 값은 그 칸만 비우고 목록은 살아남는다`() {
        // 값 확장은 계약 누락과 다르다 — 이모지 한 칸 대신 그 달 목록 전체를 날릴 일이 아니다.
        val body =
            """
            {
              "status": 200, "code": 200,
              "data": { "monthDiaryCount": 0, "weeklyDominantMood": "SMILE", "diaries": [] }
            }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)

        assertNull(decoded.data!!.weeklyDominantMood)
    }

    @Test
    fun `diaries 키가 없으면 빈 목록으로 접히지 않고 실패한다`() {
        // 0 건은 `"diaries": []` 로 온다. 키 자체가 사라진 건 계약이 바뀐 것이라,
        // 빈 목록으로 수렴시키면 "기록 없음" 화면과 구분되지 않는다 (#789).
        val body = """{ "status": 200, "code": 200, "data": { "monthDiaryCount": 0 } }"""

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(DiaryListDto.serializer()), body)
        }
    }
}
