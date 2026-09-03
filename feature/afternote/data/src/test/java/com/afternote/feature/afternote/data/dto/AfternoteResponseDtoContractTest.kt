package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
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

    /**
     * 생성에서 빈 배열은 종전의 생략과 **동작이 같다** — `PlaylistRelationStrategy.save` 는 0건을
     * 순회하고 끝나고, `PlaylistValidationStrategy.requirePlaylistSongs` 는 `null` 과 `isEmpty()` 를
     * 같이 묶어 `PLAYLIST_SONGS_REQUIRED` 로 400 을 낸다. 그래서 수정 쪽 계약(#1599)을 위해 기본값을
     * 떼도 생성 경로는 그대로다 — 달라지는 것은 키가 늘 나간다는 것뿐이고, 그것을 여기서 고정한다.
     */
    @Test
    fun `빈 playlist 생성 요청도 songs를 빈 배열로 싣는다`() {
        val request =
            CreateMemorialPayload(
                title = "추억 노트",
                memorial =
                    MemorialWritePayload(
                        memorialPhotoUrl = null,
                        songs = emptyList(),
                        memorialVideo = null,
                        memorialAudioUrl = null,
                    ),
            ).toRequest()

        val playlist =
            json
                .encodeToJsonElement(AfternoteCreatePlaylistRequestDto.serializer(), request)
                .jsonObject
                .getValue("playlist")
                .jsonObject

        assertTrue(playlist.containsKey("songs"))
        assertTrue(playlist.getValue("songs").jsonArray.isEmpty())
    }

    /**
     * 곡을 전부 뺀 수정은 **빈 배열로 삭제를 말한다** (#1599).
     *
     * BE `PlaylistRelationStrategy.update` 는 `songs != null` 일 때만 `items` 를 갈아 끼우므로,
     * 키가 빠지면 「유지」로 흡수돼 서버 곡이 그대로 남는다. 기본값 `emptyList()` 가 있던 동안
     * `encodeDefaults = false` 가 정확히 그 키를 뺐고, 곡을 전부 뺀 저장이 반영되지 않았다.
     */
    @Test
    fun `곡을 전부 뺀 수정 요청은 songs를 빈 배열로 실어 전부 삭제를 말한다`() {
        val request =
            AfternoteUpdatePayload(
                type = AfternoteType.MEMORIAL,
                title = "추억 노트",
                memorial =
                    MemorialWritePayload(
                        memorialPhotoUrl = null,
                        songs = emptyList(),
                        memorialVideo = null,
                        memorialAudioUrl = null,
                    ),
            ).toRequest()

        val encoded = json.encodeToJsonElement(AfternoteUpdateRequestDto.serializer(), request).jsonObject
        val playlist = encoded.getValue("playlist").jsonObject

        assertTrue(encoded.containsKey("playlist"))
        assertTrue(playlist.containsKey("songs"))
        assertTrue(playlist.getValue("songs").jsonArray.isEmpty())
    }

    /** 곡이 있을 때의 바디는 종전 그대로다 — 이번 변경은 빈 목록의 표현만 바꾼다 (#1599). */
    @Test
    fun `곡이 있는 수정 요청의 songs는 종전과 같은 키와 값으로 실린다`() {
        val request =
            AfternoteUpdatePayload(
                type = AfternoteType.MEMORIAL,
                title = "추억 노트",
                memorial =
                    MemorialWritePayload(
                        memorialPhotoUrl = null,
                        songs = listOf(MemorialSongPayload(title = "곡", artist = "가수", coverUrl = null)),
                        memorialVideo = null,
                        memorialAudioUrl = null,
                    ),
            ).toRequest()

        val song =
            json
                .encodeToJsonElement(AfternoteUpdateRequestDto.serializer(), request)
                .jsonObject
                .getValue("playlist")
                .jsonObject
                .getValue("songs")
                .jsonArray
                .single()
                .jsonObject

        assertEquals("곡", song.getValue("title").jsonPrimitive.content)
        assertEquals("가수", song.getValue("artist").jsonPrimitive.content)
        assertFalse(song.containsKey("coverUrl"))
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
