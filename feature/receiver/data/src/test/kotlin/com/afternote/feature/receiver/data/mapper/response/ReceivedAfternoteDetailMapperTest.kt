package com.afternote.feature.receiver.data.mapper.response

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDetailDto
import com.afternote.feature.receiver.data.dto.ReceivedCredentialsDto
import com.afternote.feature.receiver.data.dto.ReceivedMemorialVideoDto
import com.afternote.feature.receiver.data.dto.ReceivedPlaylistDto
import com.afternote.feature.receiver.data.dto.ReceivedSongDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * [ReceivedAfternoteDetailDto.toDomain] 회귀 가드 (수신자 상세).
 * 경계: createdAt null→null·있으면 포맷, 필수 type 매핑, playlist·credentials nullable 매핑,
 * memorialVideo의 video/thumbnail 추출.
 */
class ReceivedAfternoteDetailMapperTest {
    @Test
    fun `toDomain - 필드 매핑 + createdAt 포맷 + type 매핑`() {
        val result =
            ReceivedAfternoteDetailDto(
                id = 1L,
                category = "MUSIC",
                serviceName = "추모",
                senderName = "홍길동",
                createdAt = "2025-11-26T14:30:00",
                processingMethods = null,
            ).toDomain()

        assertEquals("추모", result.serviceName)
        assertEquals("홍길동", result.senderName)
        assertEquals("2025.11.26", result.createdAt)
        assertEquals(AfternoteType.MEMORIAL, result.type)
    }

    @Test
    fun `toDomain - category 를 해석할 수 없으면 실패다 - 임의의 종류로 메우지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReceivedAfternoteDetailDto(
                id = 1L,
                serviceName = "제목",
                senderName = "홍길동",
                category = null,
                processingMethods = null,
            ).toDomain()
        }
    }

    @Test
    fun `toDomain - 알 수 없는 ESTATE category 는 IllegalArgumentException 이다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReceivedAfternoteDetailDto(
                id = 1L,
                serviceName = "제목",
                senderName = "홍길동",
                category = "ESTATE",
                processingMethods = null,
            ).toDomain()
        }
    }

    @Test
    fun `toDomain - createdAt null이면 createdAt null`() {
        assertNull(
            ReceivedAfternoteDetailDto(
                id = 1L,
                serviceName = "제목",
                senderName = "홍길동",
                category = "SOCIAL",
                createdAt = null,
                processingMethods = null,
            ).toDomain().createdAt,
        )
    }

    @Test
    fun `toDomain - playlist null이면 null`() {
        assertNull(
            ReceivedAfternoteDetailDto(
                id = 1L,
                serviceName = "제목",
                senderName = "홍길동",
                category = "SOCIAL",
                playlist = null,
                processingMethods = null,
            ).toDomain().playlist,
        )
    }

    @Test
    fun `toDomain - playlist 있으면 songs·atmosphere·memorialVideo 매핑`() {
        val result =
            ReceivedAfternoteDetailDto(
                id = 1L,
                serviceName = "제목",
                senderName = "홍길동",
                category = "SOCIAL",
                processingMethods = null,
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
                serviceName = "제목",
                senderName = "홍길동",
                category = "SOCIAL",
                processingMethods = null,
                credentials = ReceivedCredentialsDto(id = "u", password = "p"),
            ).toDomain()

        assertEquals("u", result.credentials!!.id)
        assertEquals("p", result.credentials!!.password)
    }
}
