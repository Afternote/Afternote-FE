package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.BaseResponse
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 데일리질문 이미지 계약 가드 (#549).
 *
 * Swagger 전체에서 이미지 필드는 `UserResponse.profileImageUrl` 과
 * `MusicSearchItemDto.albumImageUrl` 뿐이고, 데일리질문의 요청·응답 스키마 어디에도
 * `imageUrl` 이 없다. 대신 `content` 설명이 허용 태그로 `img[src|alt|width|height|style]`
 * 을 명시한다 — 본문 이미지는 `content` HTML 에 담긴다.
 *
 * 실서버 왕복 실측(2026-08-23)에서도 `imageUrl` 은 무시되고 `<img src>` 는 그대로 살아남았다.
 */
class DailyQuestionImageContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `등록 요청에 imageUrl 을 싣지 않는다`() {
        // 계약에 없는 필드라 보내 봐야 서버가 버린다. 필드를 두면 "저장됐다" 는 착각만 남는다.
        val encoded =
            json.encodeToString(
                DailyQuestionCreateRequestDto.serializer(),
                DailyQuestionCreateRequestDto(content = "<p>본문</p>", isDraft = false, questionId = 1L),
            )

        assertFalse(encoded.contains("imageUrl"))
    }

    @Test
    fun `수정 요청에도 imageUrl 을 싣지 않는다`() {
        val encoded =
            json.encodeToString(
                DailyQuestionUpdateRequestDto.serializer(),
                DailyQuestionUpdateRequestDto(content = "<p>본문</p>", isDraft = true),
            )

        assertFalse(encoded.contains("imageUrl"))
    }

    @Test
    fun `본문의 img 태그는 응답에서 그대로 돌아온다`() {
        // 실서버 왕복 실측 형태 — 이미지가 남는 유일한 경로다.
        val body =
            """
            { "status": 200, "code": 200,
              "data": [{ "userDailyQuestionId": 26, "title": "질문", "isDraft": false,
                         "createdAt": "2026.08.23 일",
                         "receivers": [],
                         "content": "<p>사진과 함께</p><p><img src=\"https://cdn/probe.png\" alt=\"사진\" /></p>" }] }
            """.trimIndent()

        val item =
            json
                .decodeFromString(
                    BaseResponse.serializer(ListSerializer(DailyQuestionListItemDto.serializer())),
                    body,
                ).data!!
                .single()

        assertEquals(
            "<p>사진과 함께</p><p><img src=\"https://cdn/probe.png\" alt=\"사진\" /></p>",
            item.content,
        )
    }

    @Test
    fun `응답에 없는 imageUrl 키를 읽지 않아도 파싱은 멀쩡하다`() {
        // 실서버 응답에는 이 키 자체가 없다 — 읽어 봐야 항상 null 이라 썸네일이 영영 안 떴다.
        val body =
            """
            { "status": 200, "code": 200,
              "data": [{ "userDailyQuestionId": 1, "title": "질문", "content": "<p>본문</p>",
                         "createdAt": "2026.08.23 일", "isDraft": false, "receivers": [] }] }
            """.trimIndent()

        val item =
            json
                .decodeFromString(
                    BaseResponse.serializer(ListSerializer(DailyQuestionListItemDto.serializer())),
                    body,
                ).data!!
                .single()

        assertEquals(1L, item.dailyQuestionId)
    }
}
