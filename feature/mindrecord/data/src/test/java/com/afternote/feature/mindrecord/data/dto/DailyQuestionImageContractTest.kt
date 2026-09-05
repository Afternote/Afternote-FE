package com.afternote.feature.mindrecord.data.dto

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
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
 *
 * **봉투는 [requireData] 로 연다** — 프로덕션이 쓰는 그 함수다 (#1506). 종전에는 `.data!!` 로
 * 직접 꺼내, 파일 이름과 테스트 문구가 「서버 응답 계약」을 표방하면서도 실제로는 역직렬화만
 * 확인했다. 봉투가 `status != 200` 이거나 `data` 가 비어 와도 `NullPointerException` 이 날 뿐,
 * 앱이 실제로 내는 [ApiException] 과는 다른 실패가 된다. 그 두 갈래를 아래에서 직접 고정한다.
 */
class DailyQuestionImageContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
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
                ).requireData()
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
                ).requireData()
                .single()

        assertEquals(1L, item.dailyQuestionId)
    }

    @Test
    fun `봉투가 실패면 payload 가 아니라 ApiException 이 된다`() {
        // 프로덕션은 `status != 200` 을 봉투 계약 위반으로 보고 던진다. `.data!!` 로 꺼내면
        // 이 갈래가 통째로 지나가고, 화면은 «성공했는데 목록이 비었다» 로 읽는다 (#1506).
        val body =
            """
            { "status": 500, "code": 500, "message": "서버 오류", "data": null }
            """.trimIndent()

        val thrown =
            runCatching {
                json
                    .decodeFromString(
                        BaseResponse.serializer(ListSerializer(DailyQuestionListItemDto.serializer())),
                        body,
                    ).requireData()
            }.exceptionOrNull()

        assertEquals(ApiException::class.java, thrown?.javaClass)
        assertEquals(500, (thrown as ApiException).status)
    }

    @Test
    fun `봉투는 200 인데 payload 가 비면 그것도 계약 위반이다`() {
        // 서버가 성공이라 말해 놓고 data 를 안 실은 경우다. status 는 200 그대로 남긴다 —
        // 전송은 성공했고 어긴 것은 봉투 계약이라, 재시도 분기가 5xx 와 갈려야 한다.
        val body =
            """
            { "status": 200, "code": 200, "data": null }
            """.trimIndent()

        val thrown =
            runCatching {
                json
                    .decodeFromString(
                        BaseResponse.serializer(ListSerializer(DailyQuestionListItemDto.serializer())),
                        body,
                    ).requireData()
            }.exceptionOrNull()

        assertEquals(ApiException::class.java, thrown?.javaClass)
        assertEquals(200, (thrown as ApiException).status)
    }
}
