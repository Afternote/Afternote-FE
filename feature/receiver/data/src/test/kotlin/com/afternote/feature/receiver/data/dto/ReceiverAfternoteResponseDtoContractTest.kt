package com.afternote.feature.receiver.data.dto

import com.afternote.feature.receiver.data.mapper.response.toDomain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
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
            coerceInputValues = true
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
    fun `수신 상세 actions 키가 빠지면 빈 목록으로 접히지 않고 실패한다`() {
        assertMissingKey<ReceivedAfternoteDetailDto>(
            body = """{"id":1,"category":"SOCIAL","title":"t"}""",
            key = "actions",
        )
    }

    @Test
    fun `수신 상세 actions 명시적 null은 파싱되고 도메인 빈 목록으로 복구된다`() {
        val response =
            json.decodeFromString<ReceivedAfternoteDetailDto>(
                """{"id":1,"category":"SOCIAL","title":"t","actions":null}""",
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
}
