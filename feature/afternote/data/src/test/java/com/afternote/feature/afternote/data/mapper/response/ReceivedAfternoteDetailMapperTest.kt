package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDetailDto
import com.afternote.feature.afternote.data.dto.ReceivedCredentialsDto
import com.afternote.feature.afternote.data.dto.ReceivedMemorialVideoDto
import com.afternote.feature.afternote.data.dto.ReceivedPlaylistDto
import com.afternote.feature.afternote.data.dto.ReceivedSongDto
import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ReceivedAfternoteDetailDto.toDomain] 회귀 가드 (수신자 상세).
 * 경계: createdAt null→null·있으면 포맷, category null→type null, playlist·credentials nullable 매핑,
 * memorialVideo의 video/thumbnail 추출.
 */
class ReceivedAfternoteDetailMapperTest {
    @Test
    fun `toDomain - 필드 매핑 + createdAt 포맷 + type 매핑`() {
        val result =
            ReceivedAfternoteDetailDto(
                id = 1L,
                category = "MUSIC",
                title = "추모",
                senderName = "홍길동",
                createdAt = "2025-11-26T14:30:00",
            ).toDomain()

        assertEquals("추모", result.title)
        assertEquals("홍길동", result.senderName)
        assertEquals("2025.11.26", result.createdAt)
        assertEquals(AfternoteType.MEMORIAL, result.type)
    }

    @Test
    fun `toDomain - category null이면 type null`() {
        assertNull(ReceivedAfternoteDetailDto(id = 1L, category = null).toDomain().type)
    }

    @Test
    fun `toDomain - createdAt null이면 createdAt null`() {
        assertNull(ReceivedAfternoteDetailDto(id = 1L, createdAt = null).toDomain().createdAt)
    }

    @Test
    fun `toDomain - playlist null이면 null`() {
        assertNull(ReceivedAfternoteDetailDto(id = 1L, playlist = null).toDomain().playlist)
    }

    @Test
    fun `toDomain - playlist 있으면 songs·atmosphere·memorialVideo 매핑`() {
        val result =
            ReceivedAfternoteDetailDto(
                id = 1L,
                playlist =
                    ReceivedPlaylistDto(
                        atmosphere = "차분",
                        songs = listOf(ReceivedSongDto(title = "s", artist = "a", coverUrl = "c")),
                        memorialVideo = ReceivedMemorialVideoDto(videoUrl = "v", thumbnailUrl = "t"),
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
            ReceivedAfternoteDetailDto(
                id = 1L,
                credentials = ReceivedCredentialsDto(id = "u", password = "p"),
            ).toDomain()

        assertEquals("u", result.credentials!!.id)
        assertEquals("p", result.credentials!!.password)
    }
}
