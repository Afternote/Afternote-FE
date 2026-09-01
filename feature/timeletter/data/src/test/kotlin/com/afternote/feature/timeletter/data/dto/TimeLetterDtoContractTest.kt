package com.afternote.feature.timeletter.data.dto

import com.afternote.feature.timeletter.data.mapper.toDomain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 타임레터 wire DTO의 누락 키 계약 가드 (#790).
 *
 * nullable은 서버가 키를 명시한 채 `null`을 보낼 수 있다는 뜻이지, 키 생략까지 허용한다는 뜻이
 * 아니다. 프로덕션과 같은 Json 설정에서 키 누락은 실패하고 명시적 `null`과 빈 배열만 정상값으로
 * 남도록 고정한다.
 */
@OptIn(ExperimentalSerializationApi::class)
class TimeLetterDtoContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `발신 응답의 필수 키가 빠지면 빈 값으로 접히지 않고 실패한다`() {
        val complete =
            json
                .parseToJsonElement(
                    """
                    {
                      "id": 1,
                      "title": null,
                      "sendAt": null,
                      "status": "DRAFT",
                      "blocks": [],
                      "receiverIds": []
                    }
                    """.trimIndent(),
                ).jsonObject

        listOf("id", "title", "sendAt", "status", "blocks", "receiverIds").forEach { key ->
            assertMissingField<TimeLetterDto>(complete, key)
        }
    }

    @Test
    fun `발신 응답의 명시적 null과 빈 배열은 정상 파싱된다`() {
        val decoded =
            json.decodeFromString<TimeLetterDto>(
                """
                {
                  "id": 1,
                  "title": null,
                  "sendAt": null,
                  "status": "DRAFT",
                  "blocks": [],
                  "receiverIds": []
                }
                """.trimIndent(),
            )

        assertNull(decoded.title)
        assertNull(decoded.sendAt)
        assertTrue(decoded.blocks.isEmpty())
        assertTrue(decoded.receiverIds.isEmpty())
    }

    @Test
    fun `발신 응답에는 존재하지 않는 deliveredAt 없이 정상 파싱된다`() {
        val decoded =
            json.decodeFromString<TimeLetterDto>(
                """
                {
                  "id": 1,
                  "title": "미래의 나에게",
                  "sendAt": "2030-01-01T00:00:00",
                  "status": "SCHEDULED",
                  "blocks": [],
                  "receiverIds": [7]
                }
                """.trimIndent(),
            )

        assertEquals(1L, decoded.id)
    }

    @Test
    fun `응답 블록의 nullable 키가 빠지면 실패하고 명시적 null은 성공한다`() {
        val complete =
            json
                .parseToJsonElement(
                    """
                    {
                      "id": 10,
                      "blockType": "TEXT",
                      "blockOrder": 1,
                      "textContent": null,
                      "url": null,
                      "mimeType": null
                    }
                    """.trimIndent(),
                ).jsonObject

        listOf("textContent", "url", "mimeType").forEach { key ->
            assertMissingField<TimeLetterBlockDto>(complete, key)
        }

        val decoded = json.decodeFromString<TimeLetterBlockDto>(complete.toString())
        assertNull(decoded.textContent)
        assertNull(decoded.url)
        assertNull(decoded.mimeType)
    }

    @Test
    fun `수신 응답의 기본값 제거 대상 키가 빠지면 실패한다`() {
        val complete =
            json
                .parseToJsonElement(
                    """
                    {
                      "id": 1,
                      "timeLetterReceiverId": 11,
                      "title": null,
                      "blocks": [],
                      "sendAt": null,
                      "status": "SCHEDULED",
                      "senderName": null,
                      "deliveredAt": null,
                      "createdAt": null,
                      "isRead": false
                    }
                    """.trimIndent(),
                ).jsonObject

        listOf("title", "blocks", "sendAt", "senderName", "deliveredAt", "createdAt", "isRead").forEach { key ->
            assertMissingField<ReceivedTimeLetterDto>(complete, key)
        }
    }

    @Test
    fun `수신 응답의 조건부 null과 공개 전 빈 blocks는 정상 파싱된다`() {
        val decoded =
            json.decodeFromString<ReceivedTimeLetterDto>(
                """
                {
                  "id": 1,
                  "timeLetterReceiverId": 11,
                  "title": null,
                  "blocks": [],
                  "sendAt": null,
                  "status": "SCHEDULED",
                  "senderName": null,
                  "deliveredAt": null,
                  "createdAt": null,
                  "isRead": null
                }
                """.trimIndent(),
            )

        assertNull(decoded.title)
        assertTrue(decoded.blocks.isEmpty())
        assertNull(decoded.sendAt)
        assertNull(decoded.senderName)
        assertNull(decoded.deliveredAt)
        assertNull(decoded.createdAt)
        assertNull(decoded.isRead)
        assertFalse(decoded.toDomain().isRead)
    }

    @Test
    fun `생성 요청은 nullable 값과 빈 receiverIds를 모두 명시한다`() {
        val encoded =
            json.encodeToString(
                TimeLetterCreateRequestDto.serializer(),
                TimeLetterCreateRequestDto(
                    title = null,
                    sendAt = null,
                    deliveryMode = TimeLetterDeliveryModeDto.DATE,
                    status = TimeLetterStatusDto.DRAFT,
                    blocks = emptyList(),
                    receiverIds = emptyList(),
                ),
            )

        assertTrue(encoded.contains("\"title\":null"))
        assertTrue(encoded.contains("\"sendAt\":null"))
        assertTrue(encoded.contains("\"blocks\":[]"))
        assertTrue(encoded.contains("\"receiverIds\":[]"))
    }

    @Test
    fun `블록 요청은 종류별 nullable 값을 생략하지 않는다`() {
        val encoded =
            json.encodeToString(
                TimeLetterBlockRequestDto.serializer(),
                TimeLetterBlockRequestDto(
                    blockType = TimeLetterBlockTypeDto.TEXT,
                    blockOrder = 1,
                    textContent = null,
                    url = null,
                    mimeType = null,
                ),
            )

        assertTrue(encoded.contains("\"textContent\":null"))
        assertTrue(encoded.contains("\"url\":null"))
        assertTrue(encoded.contains("\"mimeType\":null"))
    }

    @Test
    fun `수정 요청은 nullable 값과 빈 blocks를 모두 명시한다`() {
        val encoded =
            json.encodeToString(
                TimeLetterUpdateRequestDto.serializer(),
                TimeLetterUpdateRequestDto(
                    title = null,
                    sendAt = null,
                    deliveryMode = null,
                    status = null,
                    blocks = emptyList(),
                ),
            )

        assertTrue(encoded.contains("\"title\":null"))
        assertTrue(encoded.contains("\"sendAt\":null"))
        assertTrue(encoded.contains("\"deliveryMode\":null"))
        assertTrue(encoded.contains("\"status\":null"))
        assertTrue(encoded.contains("\"blocks\":[]"))
    }

    private inline fun <reified T> assertMissingField(
        complete: JsonObject,
        key: String,
    ) {
        assertThrows("missing $key must fail", MissingFieldException::class.java) {
            json.decodeFromString<T>(JsonObject(complete - key).toString())
        }
    }
}
