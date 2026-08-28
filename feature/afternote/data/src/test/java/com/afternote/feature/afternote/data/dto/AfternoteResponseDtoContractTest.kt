package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** 응답 키 누락을 빈 값으로 보정하지 않고 계약 실패로 드러내는 회귀 가드 (#957). */
@OptIn(ExperimentalSerializationApi::class)
class AfternoteResponseDtoContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `작성자 상세 updatedAt 키가 빠지면 빈 문자열로 접히지 않고 실패한다`() {
        assertMissingKey<AfternoteDetailDto>(
            body =
                """{"afternoteId":1,"category":"SOCIAL","title":"t","isDraft":false,"receivers":[]}""",
            key = "updatedAt",
        )
    }

    @Test
    fun `작성자 playlist songs 키가 빠지면 빈 목록으로 접히지 않고 실패한다`() {
        assertMissingKey<AfternotePlaylistDto>(body = "{}", key = "songs")
    }

    @Test
    fun `작성자 목록 content 키가 빠지면 빈 목록으로 접히지 않고 실패한다`() {
        assertMissingKey<AfternotePageDto>(
            body = """{"page":0,"size":10,"hasNext":false}""",
            key = "content",
        )
    }

    @Test
    fun `작성자 목록 page 키가 빠지면 0으로 접히지 않고 실패한다`() {
        assertMissingKey<AfternotePageDto>(
            body = """{"content":[],"size":10,"hasNext":false}""",
            key = "page",
        )
    }

    @Test
    fun `작성자 목록 size 키가 빠지면 기본 페이지 크기로 접히지 않고 실패한다`() {
        assertMissingKey<AfternotePageDto>(
            body = """{"content":[],"page":0,"hasNext":false}""",
            key = "size",
        )
    }

    @Test
    fun `작성자 목록 hasNext 키가 빠지면 false로 접혀 페이지네이션이 멈추지 않고 실패한다`() {
        assertMissingKey<AfternotePageDto>(
            body = """{"content":[],"page":0,"size":10}""",
            key = "hasNext",
        )
    }

    @Test
    fun `음악 검색 tracks 키가 빠지면 빈 검색 결과로 접히지 않고 실패한다`() {
        assertMissingKey<MusicSearchResponseDto>(body = "{}", key = "tracks")
    }

    @Test
    fun `빈 playlist 생성 요청은 songs를 보내지 않는다`() {
        val request =
            CreateMemorialPayload(
                title = "추억 노트",
                memorial = MemorialWritePayload(),
            ).toRequest()

        val playlist =
            json
                .encodeToJsonElement(AfternoteCreatePlaylistRequestDto.serializer(), request)
                .jsonObject
                .getValue("playlist")
                .jsonObject

        assertFalse(playlist.containsKey("songs"))
    }

    @Test
    fun `빈 playlist 수정 요청은 playlist는 보내되 songs는 보내지 않는다`() {
        val request =
            AfternoteUpdatePayload(
                type = AfternoteType.MEMORIAL,
                title = "추억 노트",
                memorial = MemorialWritePayload(),
            ).toRequest()

        val encoded = json.encodeToJsonElement(AfternoteUpdateRequestDto.serializer(), request).jsonObject
        val playlist = encoded.getValue("playlist").jsonObject

        assertTrue(encoded.containsKey("playlist"))
        assertFalse(playlist.containsKey("songs"))
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
