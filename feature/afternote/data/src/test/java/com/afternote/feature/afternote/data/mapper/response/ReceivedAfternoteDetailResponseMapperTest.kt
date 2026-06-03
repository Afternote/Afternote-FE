package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDetailResponse
import com.afternote.feature.afternote.data.dto.ReceivedCredentialsInfo
import com.afternote.feature.afternote.data.dto.ReceivedMemorialVideoInfo
import com.afternote.feature.afternote.data.dto.ReceivedPlaylistInfo
import com.afternote.feature.afternote.data.dto.ReceivedSongInfo
import com.afternote.feature.afternote.domain.AfternoteServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ReceivedAfternoteDetailResponse.toDomain] 회귀 가드 (수신자 상세).
 * 경계: createdAt null→null·있으면 포맷, category null→type null, playlist·credentials nullable 매핑,
 * memorialVideo의 video/thumbnail 추출.
 */
class ReceivedAfternoteDetailResponseMapperTest {
    @Test
    fun `toDomain - 필드 매핑 + createdAt 포맷 + type 매핑`() {
        val result =
            ReceivedAfternoteDetailResponse(
                id = 1L,
                category = "MUSIC",
                title = "추모",
                senderName = "홍길동",
                createdAt = "2025-11-26T14:30:00",
            ).toDomain()

        assertEquals("추모", result.title)
        assertEquals("홍길동", result.senderName)
        assertEquals("2025.11.26", result.createdAt)
        assertEquals("MUSIC", result.category)
        assertEquals(AfternoteServiceType.MEMORIAL, result.type)
    }

    @Test
    fun `toDomain - category null이면 type null`() {
        assertNull(ReceivedAfternoteDetailResponse(id = 1L, category = null).toDomain().type)
    }

    @Test
    fun `toDomain - createdAt null이면 createdAt null`() {
        assertNull(ReceivedAfternoteDetailResponse(id = 1L, createdAt = null).toDomain().createdAt)
    }

    @Test
    fun `toDomain - playlist null이면 null`() {
        assertNull(ReceivedAfternoteDetailResponse(id = 1L, playlist = null).toDomain().playlist)
    }

    @Test
    fun `toDomain - playlist 있으면 songs·atmosphere·memorialVideo 매핑`() {
        val result =
            ReceivedAfternoteDetailResponse(
                id = 1L,
                playlist =
                    ReceivedPlaylistInfo(
                        atmosphere = "차분",
                        songs = listOf(ReceivedSongInfo(title = "s", artist = "a", coverUrl = "c")),
                        memorialVideo = ReceivedMemorialVideoInfo(videoUrl = "v", thumbnailUrl = "t"),
                    ),
            ).toDomain()

        val playlist = result.playlist!!
        assertEquals("차분", playlist.atmosphere)
        assertEquals(1, playlist.songs.size)
        assertEquals("v", playlist.memorialVideoUrl)
        assertEquals("t", playlist.memorialThumbnailUrl)
    }

    @Test
    fun `toDomain - credentials 매핑`() {
        val result =
            ReceivedAfternoteDetailResponse(
                id = 1L,
                credentials = ReceivedCredentialsInfo(id = "u", password = "p"),
            ).toDomain()

        assertEquals("u", result.credentials!!.id)
        assertEquals("p", result.credentials!!.password)
    }
}
