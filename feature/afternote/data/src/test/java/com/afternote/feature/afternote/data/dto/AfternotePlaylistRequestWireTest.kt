package com.afternote.feature.afternote.data.dto

import com.afternote.core.network.di.NetworkModule
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 추억 노트 수정 요청의 **키 유무**를 고정한다 (#1596).
 *
 * BE 는 「키 없음 = 유지」와 「키 있고 null = 삭제」를 가르므로(Afternote-BE `72fee63`),
 * 이 계약은 값이 아니라 *직렬화 설정* 위에 서 있다. 그래서 테스트용 [kotlinx.serialization.json.Json]
 * 을 새로 만들지 않고 **앱이 실제로 쓰는** [NetworkModule.provideJson] 을 그대로 태운다 — 누가
 * `encodeDefaults` · `explicitNulls` 를 건드리면 여기서 깨져야 한다.
 */
class AfternotePlaylistRequestWireTest {
    private val json = NetworkModule.provideJson()

    private fun updateBody(memorial: MemorialWritePayload?) =
        json
            .encodeToJsonElement(
                AfternoteUpdatePayload(
                    type = if (memorial == null) AfternoteType.SOCIAL_NETWORK else AfternoteType.MEMORIAL,
                    title = "제목",
                    memorial = memorial,
                ).toRequest(),
            ).jsonObject

    private fun playlistOf(memorial: MemorialWritePayload) = updateBody(memorial).getValue("playlist").jsonObject

    private val filled =
        MemorialWritePayload(
            memorialPhotoUrl = "https://cdn.test/afternotes/photo.jpg",
            songs = listOf(MemorialSongPayload(title = "곡", artist = "가수", coverUrl = null)),
            memorialVideo =
                MemorialVideoPayload(
                    videoUrl = "https://cdn.test/afternotes/video.mp4",
                    thumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                ),
        )

    private val emptied = MemorialWritePayload(memorialPhotoUrl = null, songs = emptyList(), memorialVideo = null)

    /** 발행된 PLAYLIST PATCH 검증을 통과하도록 기존 곡을 함께 싣는 실제 서버 미디어 삭제 스냅샷. */
    private val deletedServerMedia =
        MemorialWritePayload(
            memorialPhotoUrl = null,
            songs = listOf(MemorialSongPayload(title = "기존 곡", artist = "기존 가수", coverUrl = null)),
            memorialVideo = null,
        )

    @Test
    fun `영정사진이 비면 키를 남긴 채 null 이 실려 삭제로 나간다`() {
        val playlist = playlistOf(emptied)

        assertTrue("memorialPhotoUrl" in playlist)
        assertEquals(JsonNull, playlist.getValue("memorialPhotoUrl"))
    }

    @Test
    fun `추모 영상이 비면 키를 남긴 채 null 이 실려 삭제로 나간다`() {
        val playlist = playlistOf(emptied)

        assertTrue("memorialVideo" in playlist)
        assertEquals(JsonNull, playlist.getValue("memorialVideo"))
    }

    @Test
    fun `서버 미디어 삭제 저장은 두 null 과 기존 곡을 한 playlist 에 싣는다`() {
        val playlist = playlistOf(deletedServerMedia)

        assertEquals(JsonNull, playlist.getValue("memorialPhotoUrl"))
        assertEquals(JsonNull, playlist.getValue("memorialVideo"))
        val song =
            playlist
                .getValue("songs")
                .jsonArray
                .single()
                .jsonObject
        assertEquals("기존 곡", song.getValue("title").jsonPrimitive.content)
        assertEquals("기존 가수", song.getValue("artist").jsonPrimitive.content)
    }

    /**
     * 곡은 `null` 이 아니라 **빈 배열**로 삭제를 말한다 (#1599).
     *
     * BE `PlaylistRelationStrategy.update` 는 `songs != null` 일 때 `playlist.getItems().clear()` 로
     * 통째로 갈아 끼우므로 `[]` 가 곧 전부 삭제다. 기본값 `emptyList()` 가 있던 동안은 바로 그
     * 배열이 `encodeDefaults = false` 때문에 키째 빠져 「유지」로 흡수됐다.
     */
    @Test
    fun `곡을 전부 빼면 빈 배열이 실려 전부 삭제로 나간다`() {
        val playlist = playlistOf(emptied)

        assertTrue("songs" in playlist)
        assertTrue(playlist.getValue("songs").jsonArray.isEmpty())
    }

    /**
     * 같은 바디 안에서 생략이 **여전히 살아 있음**을 함께 못박는다 — 위 세 건이 「값을 실었다」가
     * 아니라 「기본값을 뗐다」로 성립한다는 증거다. FE 가 그리지 않는 슬롯은 계속 빠져야 한다.
     */
    @Test
    fun `FE 가 모델링하지 않는 슬롯은 키째 빠져 유지로 나간다`() {
        val playlist = playlistOf(emptied)

        assertFalse("atmosphere" in playlist)
        assertFalse("memorialAudioUrl" in playlist)
    }

    @Test
    fun `값이 있으면 종전과 같은 키로 그대로 실린다`() {
        val playlist = playlistOf(filled)

        assertEquals(
            "https://cdn.test/afternotes/photo.jpg",
            playlist.getValue("memorialPhotoUrl").jsonPrimitive.content,
        )
        val video = playlist.getValue("memorialVideo").jsonObject
        assertEquals("https://cdn.test/afternotes/video.mp4", video.getValue("videoUrl").jsonPrimitive.content)
        assertEquals("https://cdn.test/afternotes/thumb.jpg", video.getValue("thumbnailUrl").jsonPrimitive.content)
        val song =
            playlist
                .getValue("songs")
                .jsonArray
                .single()
                .jsonObject
        assertEquals("곡", song.getValue("title").jsonPrimitive.content)
    }

    /** 추억 노트가 아닌 수정은 플레이리스트를 **말하지 않는다** — 키가 나가면 남의 미디어를 지운다. */
    @Test
    fun `추억 노트가 아닌 수정에는 playlist 키 자체가 없다`() {
        assertFalse("playlist" in updateBody(memorial = null))
    }
}
