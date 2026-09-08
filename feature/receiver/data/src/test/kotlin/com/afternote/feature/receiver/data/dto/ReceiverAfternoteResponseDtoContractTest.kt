package com.afternote.feature.receiver.data.dto

import com.afternote.feature.receiver.data.mapper.response.toDomain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** 수신 응답 키 누락과 명시적 null을 구분하는 계약 회귀 가드 (#957). */
@OptIn(ExperimentalSerializationApi::class)
class ReceiverAfternoteResponseDtoContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `수신 목록 afternotes 키가 빠지면 빈 목록으로 접히지 않고 실패한다`() {
        assertMissingKey<ReceivedAfternoteListDto>(body = """{"totalCount":0}""", key = "afternotes")
    }

    @Test
    fun `수신 목록 totalCount 키가 빠지면 0으로 접히지 않고 실패한다`() {
        assertMissingKey<ReceivedAfternoteListDto>(body = """{"afternotes":[]}""", key = "totalCount")
    }

    @Test
    fun `수신 목록 항목을 직접 디코딩하면 category 키 누락은 계약 위반으로 실패한다`() {
        assertSerializationFailure<ReceivedAfternoteDto>(
            body = """{"id":1,"title":"소셜 계정 정리"}""",
            key = "category",
        )
    }

    @Test
    fun `수신 목록 항목을 직접 디코딩하면 category 명시적 null도 계약 위반으로 실패한다`() {
        assertSerializationFailure<ReceivedAfternoteDto>(
            body = """{"id":1,"title":"소셜 계정 정리","category":null}""",
            key = "category",
        )
    }

    @Test
    fun `수신 목록은 category 디코딩에 실패한 항목만 제외하고 계약상 논널 DTO를 보존한다`() {
        val response =
            json.decodeFromString<ReceivedAfternoteListDto>(
                """
                {
                  "afternotes": [
                    {"id":1,"title":"소셜 계정","category":"SOCIAL"},
                    {"id":2,"title":"category 누락"},
                    {"id":3,"title":"category null","category":null},
                    {"id":4,"title":"미지원 category","category":"ESTATE"},
                    {"id":5,"title":"사업자 항목","category":"BUSINESS"}
                  ],
                  "totalCount": 9
                }
                """.trimIndent(),
            )

        assertEquals(listOf(1L, 4L, 5L), response.afternotes.map { it.id })
        assertEquals(listOf("SOCIAL", "ESTATE", "BUSINESS"), response.afternotes.map { it.category })
        assertEquals(2, response.decodingRejectedItemCount)
        assertEquals(9, response.totalCount)
    }

    @Test
    fun `수신 목록 afternotes 자체의 잘못된 shape는 빈 목록으로 접히지 않고 실패한다`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<ReceivedAfternoteListDto>(
                """{"afternotes":{},"totalCount":0}""",
            )
        }
    }

    @Test
    fun `수신 목록의 한 항목이 객체가 아니면 목록 전체 계약 위반으로 실패한다`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<ReceivedAfternoteListDto>(
                """{"afternotes":[{"id":1,"title":"t","category":"SOCIAL"},42],"totalCount":2}""",
            )
        }
    }

    @Test
    fun `category가 누락 또는 null이어도 다른 필수 필드까지 누락되면 목록 전체가 실패한다`() {
        val combinedContractViolations =
            listOf(
                """{"afternotes":[{"title":"id와 category 누락"}],"totalCount":1}""",
                """{"afternotes":[{"id":1,"category":null}],"totalCount":1}""",
            )

        combinedContractViolations.forEach { body ->
            assertThrows(SerializationException::class.java) {
                json.decodeFromString<ReceivedAfternoteListDto>(body)
            }
        }
    }

    @Test
    fun `category가 숫자나 객체면 필수 필드가 정상인 해당 원소만 제외한다`() {
        val response =
            json.decodeFromString<ReceivedAfternoteListDto>(
                """
                {
                  "afternotes": [
                    {"id":1,"title":"소셜 계정","category":"SOCIAL"},
                    {"id":2,"title":"숫자 category","category":42},
                    {"id":3,"title":"객체 category","category":{"value":"BUSINESS"}},
                    {"id":4,"title":"사업자 항목","category":"BUSINESS"}
                  ],
                  "totalCount": 4
                }
                """.trimIndent(),
            )

        assertEquals(listOf(1L, 4L), response.afternotes.map { it.id })
        assertEquals(2, response.decodingRejectedItemCount)
        assertEquals(4, response.totalCount)
    }

    @Test
    fun `수신 목록 항목의 category 외 필수 필드 누락은 목록 전체 계약 위반으로 실패한다`() {
        assertSerializationFailure<ReceivedAfternoteListDto>(
            body = """{"afternotes":[{"title":"id 누락","category":"SOCIAL"}],"totalCount":1}""",
            key = "id",
        )
    }

    @Test
    fun `수신 목록 인코딩은 내부 디코딩 거절 개수를 wire에 노출하지 않는다`() {
        val encoded =
            json.encodeToString(
                ReceivedAfternoteListDto(
                    afternotes =
                        listOf(
                            ReceivedAfternoteDto(
                                id = 1L,
                                title = "소셜 계정",
                                category = "SOCIAL",
                            ),
                        ),
                    totalCount = 3,
                    decodingRejectedItemCount = 2,
                ),
            )

        assertTrue("decodingRejectedItemCount" !in encoded)
        val roundTrip = json.decodeFromString<ReceivedAfternoteListDto>(encoded)
        assertEquals(listOf(1L), roundTrip.afternotes.map { it.id })
        assertEquals(3, roundTrip.totalCount)
    }

    @Test
    fun `수신 상세 actions 키가 빠지면 빈 목록으로 접히지 않고 실패한다`() {
        assertMissingKey<ReceivedAfternoteDetailDto>(
            body = """{"id":1,"category":"SOCIAL","title":"t","senderName":"홍길동"}""",
            key = "actions",
        )
    }

    @Test
    fun `수신 상세 actions 명시적 null은 파싱되고 도메인 빈 목록으로 복구된다`() {
        val response =
            json.decodeFromString<ReceivedAfternoteDetailDto>(
                """{"id":1,"category":"SOCIAL","title":"t","senderName":"홍길동","actions":null}""",
            )

        assertNull(response.processingMethods)
        assertEquals(emptyList<String>(), response.toDomain().processingMethods)
    }

    @Test
    fun `수신 playlist songs 키가 빠지면 빈 목록으로 접히지 않고 실패한다`() {
        assertMissingKey<ReceivedPlaylistDto>(body = "{}", key = "songs")
    }

    private inline fun <reified T> assertMissingKey(
        body: String,
        key: String,
    ) {
        val failure =
            assertThrows(MissingFieldException::class.java) {
                json.decodeFromString<T>(body)
            }

        assertTrue(failure.message.orEmpty().contains(key))
    }

    private inline fun <reified T> assertSerializationFailure(
        body: String,
        key: String,
    ) {
        val failure =
            assertThrows(SerializationException::class.java) {
                json.decodeFromString<T>(body)
            }

        assertTrue(failure.message.orEmpty().contains(key))
    }
}
