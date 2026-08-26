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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AfternoteDetailDto.toDetailDomain] 회귀 가드 (작성자 상세).
 * 핵심 경계: receivers null→emptyList, receiver 필드 null→"", processingMethods null→emptyList,
 * memorialVideo null→video/thumbnail null,
 * credentials·memorial nullable 매핑.
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
        assertNull(result.memorial)
        assertTrue(result.processingMethods.isEmpty())
    }

    @Test
    fun `toDetailDomain - 사업자 상세는 BUSINESS 로 올라온다 - 소셜로 둔갑하지 않는다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "BUSINESS",
                title = "t",
            ).toDetailDomain()

        assertEquals(AfternoteType.BUSINESS, result.type)
    }

    @Test
    fun `toDetailDomain - 해석할 수 없는 category 는 실패다 - 임의의 종류로 메우지 않는다`() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                AfternoteDetailDto(
                    afternoteId = 42L,
                    category = "???",
                    title = "t",
                ).toDetailDomain()
            }

        assertTrue(
            "진단에 식별자와 원본 값이 남아야 한다",
            thrown.message!!.contains("42") && thrown.message!!.contains("???"),
        )
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

    /**
     * DTO 는 방어적으로 receiverId 가 nullable 이지만 서버 스펙상 필수다(상세 응답 `ReceiverRequest` 는
     * 이 필드 하나뿐). 도메인은 non-null 이므로 경계인 이 매퍼가 걸러야 한다.
     */
    @Test
    fun `toDetailDomain - receiverId 없는 항목은 도메인으로 올리지 않는다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
                receivers =
                    listOf(
                        AfternoteDetailReceiverDto(receiverId = null, name = "식별자없음"),
                        AfternoteDetailReceiverDto(receiverId = 9L, name = "김수신"),
                    ),
            ).toDetailDomain()

        assertEquals(listOf(9L), result.receivers.map { it.receiverId })
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
    fun `toDetailDomain - playlist 미디어·곡 매핑`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                memorial =
                    AfternotePlaylistDto(
                        memorialPhotoUrl = "memorial.jpg",
                        songs = listOf(AfternoteSongDto(id = 3L, title = "s", artist = "a")),
                        memorialVideo = AfternoteMemorialVideoDto(videoUrl = "v.mp4", thumbnailUrl = "t.jpg"),
                    ),
            ).toDetailDomain()

        val media = result.memorial!!.media
        assertEquals("memorial.jpg", media.photoUrl)
        assertEquals("v.mp4", media.videoUrl)
        assertEquals("t.jpg", media.thumbnailUrl)
        assertEquals(1, result.memorial!!.songs.size)
        assertEquals(
            3L,
            result.memorial!!
                .songs
                .single()
                .id,
        )
    }

    @Test
    fun `toDetailDomain - memorialVideo null이면 video thumbnail null`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                memorial = AfternotePlaylistDto(memorialVideo = null),
            ).toDetailDomain()

        val media = result.memorial!!.media
        assertNull(media.videoUrl)
        assertNull(media.thumbnailUrl)
    }
}
