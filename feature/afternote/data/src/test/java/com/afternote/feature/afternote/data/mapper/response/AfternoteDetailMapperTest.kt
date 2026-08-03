package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.AfternoteCredentialsDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailReceiverDto
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideoDto
import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AfternoteDetailDto.toDetailDomain] 회귀 가드 (작성자 상세).
 * 핵심 경계: receivers null→emptyList, receiver 필드 null→"", actions null→emptyList,
 * memorialPhotoUrl 없으면 profilePhoto로 대체, memorialVideo null→video/thumbnail null,
 * credentials·playlist nullable 매핑.
 */
class AfternoteDetailMapperTest {
    @Test
    fun `toDetailDomain - 최소 응답은 nullable이 비거나 null`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
            ).toDetailDomain()

        assertEquals(1L, result.id)
        assertEquals(AfternoteType.SOCIAL_NETWORK, result.type)
        assertTrue(result.receivers.isEmpty())
        assertNull(result.credentials)
        assertNull(result.playlist)
        assertTrue(result.processing!!.actions.isEmpty())
    }

    @Test
    fun `toDetailDomain - timestamps 포맷`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                createdAt = "2025-11-26T14:30:00",
                updatedAt = "2025-12-01T09:00:00",
            ).toDetailDomain()

        assertEquals("2025.11.26", result.timestamps.createdAt)
        assertEquals("2025.12.01", result.timestamps.updatedAt)
    }

    @Test
    fun `toDetailDomain - receiver의 null 필드는 빈 문자열`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
                receivers = listOf(AfternoteDetailReceiverDto(receiverId = 5L)),
            ).toDetailDomain()

        val receiver = result.receivers.single()
        assertEquals(5L, receiver.receiverId)
        assertEquals("", receiver.name)
        assertEquals("", receiver.relation)
        assertEquals("", receiver.phone)
    }

    @Test
    fun `toDetailDomain - credentials 매핑`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                credentials = AfternoteCredentialsDto(id = "user", password = "pw"),
            ).toDetailDomain()

        assertEquals("user", result.credentials!!.id)
        assertEquals("pw", result.credentials!!.password)
    }

    @Test
    fun `toDetailDomain - memorialPhotoUrl 없으면 profilePhoto로 대체`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                playlist =
                    AfternotePlaylistDto(
                        profilePhoto = "profile.jpg",
                        memorialPhotoUrl = null,
                        songs = listOf(AfternoteSongDto(id = 3L, title = "s", artist = "a")),
                        memorialVideo = AfternoteMemorialVideoDto(videoUrl = "v.mp4", thumbnailUrl = "t.jpg"),
                    ),
            ).toDetailDomain()

        val media = result.playlist!!.playlistDetailMemorialMedia
        assertEquals("profile.jpg", media.photoUrl)
        assertEquals("v.mp4", media.videoUrl)
        assertEquals("t.jpg", media.thumbnailUrl)
        assertEquals(1, result.playlist!!.songs.size)
        assertEquals(
            3L,
            result.playlist!!
                .songs
                .single()
                .id,
        )
    }

    @Test
    fun `toDetailDomain - memorialPhotoUrl 있으면 그대로`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                playlist = AfternotePlaylistDto(profilePhoto = "profile.jpg", memorialPhotoUrl = "memorial.jpg"),
            ).toDetailDomain()

        assertEquals("memorial.jpg", result.playlist!!.playlistDetailMemorialMedia.photoUrl)
    }

    @Test
    fun `toDetailDomain - memorialVideo null이면 video thumbnail null`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                playlist = AfternotePlaylistDto(profilePhoto = "p", memorialVideo = null),
            ).toDetailDomain()

        val media = result.playlist!!.playlistDetailMemorialMedia
        assertNull(media.videoUrl)
        assertNull(media.thumbnailUrl)
    }
}
