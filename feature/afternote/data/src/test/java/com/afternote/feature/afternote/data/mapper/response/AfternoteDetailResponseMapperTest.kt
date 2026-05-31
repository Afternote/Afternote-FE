package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.AfternoteCredentials
import com.afternote.feature.afternote.data.dto.AfternoteDetailReceiver
import com.afternote.feature.afternote.data.dto.AfternoteDetailResponse
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideo
import com.afternote.feature.afternote.data.dto.AfternotePlaylist
import com.afternote.feature.afternote.data.dto.AfternoteSong
import com.afternote.feature.afternote.domain.AfternoteServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AfternoteDetailResponse.toDetailDomain] 회귀 가드 (작성자 상세).
 * 핵심 경계: receivers null→emptyList, receiver 필드 null→"", actions null→emptyList,
 * memorialPhotoUrl 없으면 profilePhoto로 대체, memorialVideo null→video/thumbnail null,
 * credentials·playlist nullable 매핑.
 */
class AfternoteDetailResponseMapperTest {
    @Test
    fun `toDetailDomain - 최소 응답은 nullable이 비거나 null`() {
        val result =
            AfternoteDetailResponse(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
            ).toDetailDomain()

        assertEquals(1L, result.id)
        assertEquals(AfternoteServiceType.SOCIAL_NETWORK, result.type)
        assertTrue(result.receivers.isEmpty())
        assertNull(result.credentials)
        assertNull(result.playlist)
        assertTrue(result.processing!!.actions.isEmpty())
    }

    @Test
    fun `toDetailDomain - timestamps 포맷`() {
        val result =
            AfternoteDetailResponse(
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
            AfternoteDetailResponse(
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
                receivers = listOf(AfternoteDetailReceiver(receiverId = 5L)),
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
            AfternoteDetailResponse(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                credentials = AfternoteCredentials(id = "user", password = "pw"),
            ).toDetailDomain()

        assertEquals("user", result.credentials!!.id)
        assertEquals("pw", result.credentials!!.password)
    }

    @Test
    fun `toDetailDomain - memorialPhotoUrl 없으면 profilePhoto로 대체`() {
        val result =
            AfternoteDetailResponse(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                playlist =
                    AfternotePlaylist(
                        profilePhoto = "profile.jpg",
                        memorialPhotoUrl = null,
                        songs = listOf(AfternoteSong(id = 3L, title = "s", artist = "a")),
                        memorialVideo = AfternoteMemorialVideo(videoUrl = "v.mp4", thumbnailUrl = "t.jpg"),
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
            AfternoteDetailResponse(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                playlist = AfternotePlaylist(profilePhoto = "profile.jpg", memorialPhotoUrl = "memorial.jpg"),
            ).toDetailDomain()

        assertEquals("memorial.jpg", result.playlist!!.playlistDetailMemorialMedia.photoUrl)
    }

    @Test
    fun `toDetailDomain - memorialVideo null이면 video thumbnail null`() {
        val result =
            AfternoteDetailResponse(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                playlist = AfternotePlaylist(profilePhoto = "p", memorialVideo = null),
            ).toDetailDomain()

        val media = result.playlist!!.playlistDetailMemorialMedia
        assertNull(media.videoUrl)
        assertNull(media.thumbnailUrl)
    }
}
