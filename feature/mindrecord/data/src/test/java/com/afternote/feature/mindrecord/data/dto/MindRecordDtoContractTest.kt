package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * DTO 기본값·nullable 정리 계약 가드 (#789).
 *
 * 서버가 항상 채우는 응답 키를 `false`·`0`·`""`·`emptyList()` 로 보정하면, 응답 키 누락과
 * 계약 변경이 파싱 실패가 아니라 **정상적인 빈 값**으로 바뀐다. 이 파일은 그 경계를 고정한다.
 *
 * - 키 누락 → `MissingFieldException` (계약이 바뀐 것)
 * - 실제 의미가 있는 명시적 `null` → 정상 파싱 (값이 없는 상태)
 * - 클라가 모르는 기분 **값** → 그 칸만 비움 (표기가 늘어난 것)
 *
 * Json 설정은 `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys).
 */
class MindRecordDtoContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    // ---------- 데일리질문 목록 ----------

    @Test
    fun `데일리질문 목록의 isDraft 가 빠지면 실패한다`() {
        // `false` 로 접히면 임시저장 답변이 목록에 노출된다.
        val body =
            """
            { "status": 200, "code": 200,
              "data": [{ "userDailyQuestionId": 1, "title": "t", "content": "c",
                         "createdAt": "2026.08.23 일" }] }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(ListSerializerOf), body)
        }
    }

    @Test
    fun `계약에 없는 imageUrl 은 빠져도 파싱을 깨지 않는다`() {
        // Swagger `DailyQuestionListResponse` 에 없는 필드 — 기본값을 유지하는 유일한 자리.
        val body =
            """
            { "status": 200, "code": 200,
              "data": [{ "userDailyQuestionId": 1, "title": "t", "content": "c",
                         "createdAt": "2026.08.23 일", "isDraft": false, "receivers": [] }] }
            """.trimIndent()

        val decoded = json.decodeFromString(BaseResponse.serializer(ListSerializerOf), body)

        // imageUrl 은 계약에 없어 DTO 에서 걷었다 — 썸네일은 본문 img 태그에서 뽑는다 (#549).
        assertEquals("t", decoded.data!!.single().title)
    }

    // ---------- 수신자 목록 ----------

    @Test
    fun `수신자 일기 목록은 발신자와 같은 계약이라 키가 빠지면 실패한다`() {
        // 두 API 모두 Swagger `DiaryResponse` 를 쓴다 — nullable 과 기본값도 같이 맞춘다.
        val body =
            """
            { "status": 200, "code": 200,
              "data": { "diaries": [{ "diaryId": 1, "title": "t", "content": "c",
                        "isDraft": false, "createdAt": "2026.08.23 일", "updatedAt": "2026.08.23 일" }] } }
            """.trimIndent()

        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(BaseResponse.serializer(ReceiverDiaryListDto.serializer()), body)
        }
    }

    @Test
    fun `수신자 일기 목록 전체 필드는 정상 파싱된다`() {
        val body =
            """
            { "status": 200, "code": 200,
              "data": { "diaries": [{ "diaryId": 1, "title": "t", "content": "c",
                        "isDraft": false, "todayMood": "HAPPY", "date": "2026-08-23",
                        "createdAt": "2026.08.23 일", "updatedAt": "2026.08.23 일" }] } }
            """.trimIndent()

        val diary =
            json
                .decodeFromString(BaseResponse.serializer(ReceiverDiaryListDto.serializer()), body)
                .data!!
                .diaries
                .single()

        assertEquals(TodayMoodDto.HAPPY, diary.todayMood)
        assertEquals("2026-08-23", diary.date)
    }

    @Test
    fun `수신자 목록의 래퍼 키가 빠지면 빈 목록으로 접히지 않고 실패한다`() {
        assertThrows(MissingFieldException::class.java) {
            json.decodeFromString(
                BaseResponse.serializer(ReceiverDailyQuestionListDto.serializer()),
                """{ "status": 200, "code": 200, "data": { } }""",
            )
        }
    }

    // ---------- 요청 직렬화 ----------

    @Test
    fun `수신자를 고르지 않으면 빈 목록이 그대로 나간다`() {
        // 생성 API 는 `null` 과 `[]` 를 모두 "수신자 없음" 으로 정규화한다. 작성 UI 가 항상
        // 목록을 갖고 있으므로 nullable 로 낮추지 않고 빈 목록을 보낸다 (#789).
        val encoded =
            json.encodeToString(
                DiaryCreateRequestDto.serializer(),
                DiaryCreateRequestDto(
                    title = "t",
                    content = "c",
                    isDraft = false,
                    todayMood = TodayMoodDto.SOSO,
                    receiverIds = emptyList(),
                    date = "2026-08-01",
                ),
            )

        assertEquals(true, encoded.contains("\"receiverIds\":[]"))
    }

    @Test
    fun `생성은 기록일을 항상 싣고 수정은 생략할 수 있다`() {
        // 서버는 생성에서 미전송이면 오늘로 채우고, 수정에서 생략이면 기존 값을 유지한다
        // (Afternote-BE#244, PR #262). 그 차이가 타입에 그대로 드러나야 한다 (#1008).
        val created =
            json.encodeToString(
                DiaryCreateRequestDto.serializer(),
                DiaryCreateRequestDto(
                    title = "t",
                    content = "c",
                    isDraft = false,
                    todayMood = TodayMoodDto.SOSO,
                    receiverIds = emptyList(),
                    date = "2026-08-01",
                ),
            )
        assertEquals(true, created.contains("\"date\":\"2026-08-01\""))

        val omitted =
            json.encodeToString(
                DiaryUpdateRequestDto.serializer(),
                DiaryUpdateRequestDto(
                    title = "t",
                    content = "c",
                    isDraft = false,
                    todayMood = TodayMoodDto.SOSO,
                    receiverIds = null,
                    date = null,
                ),
            )
        assertEquals(false, omitted.contains("\"date\""))
    }

    @Test
    fun `PATCH 는 생략과 명시적 null 이 코드에서 구분된다`() {
        // 선택 필드라 nullable 은 유지하되 기본값은 없다 — 호출부가 모든 인자를 적어야 한다.
        val encoded =
            json.encodeToString(
                DailyQuestionUpdateRequestDto.serializer(),
                DailyQuestionUpdateRequestDto(
                    content = "수정 본문",
                    isDraft = false,
                    date = null,
                    questionId = null,
                ),
            )

        assertEquals(true, encoded.contains("\"content\":\"수정 본문\""))
        assertEquals(true, encoded.contains("\"isDraft\":false"))
    }

    private companion object {
        val ListSerializerOf = kotlinx.serialization.builtins.ListSerializer(DailyQuestionListItemDto.serializer())
    }
}
