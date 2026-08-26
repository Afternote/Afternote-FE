package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.AfternoteCredentialsDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailReceiverDto
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideoDto
import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.data.dto.LeaveMessageBlockDto
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.DetailContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AfternoteDetailDto.toDetailDomain] 회귀 가드 (작성자 상세).
 * 핵심 경계: 공통 필드와 타입별 [DetailContent] 분리, 부분·부재 credentials 의 빈 값 강등,
 * receivers null→emptyList, receiver 필드 null→"", processingMethods null→emptyList,
 * memorialVideo null→video/thumbnail null.
 */
class AfternoteDetailMapperTest {
    @Test
    fun `toDetailDomain - gallery 최소 응답은 컬렉션이 비어 있다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
            ).toDetailDomain()

        assertEquals(1L, result.id)
        val content = result.content as DetailContent.Gallery
        assertEquals(AfternoteType.GALLERY_AND_FILES, content.type)
        assertTrue(result.receivers.isEmpty())
        assertTrue(content.processingMethods.isEmpty())
    }

    @Test
    fun `toDetailDomain - 사업자 상세는 BUSINESS 로 올라온다 - 소셜로 둔갑하지 않는다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "BUSINESS",
                title = "t",
            ).toDetailDomain()

        assertTrue("사업자는 Business 내용으로 올라와야 한다", result.content is DetailContent.Business)
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
                category = "GALLERY",
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
        assertTrue(result.content is DetailContent.Gallery)
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

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("user", credentials.id)
        assertEquals("pw", credentials.password)
    }

    @Test
    fun `toDetailDomain - credentials가 아예 없으면 빈 값으로 낮춘다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
            ).toDetailDomain()

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("", credentials.id)
        assertEquals("", credentials.password)
    }

    @Test
    fun `toDetailDomain - credentials id만 없으면 id를 빈 값으로 낮춘다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                credentials = AfternoteCredentialsDto(id = null, password = "pw"),
            ).toDetailDomain()

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("", credentials.id)
        assertEquals("pw", credentials.password)
    }

    @Test
    fun `toDetailDomain - credentials password만 없으면 password를 빈 값으로 낮춘다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                credentials = AfternoteCredentialsDto(id = "user", password = null),
            ).toDetailDomain()

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("user", credentials.id)
        assertEquals("", credentials.password)
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
                        songs = listOf(AfternoteSongDto(title = "s", artist = "a")),
                        memorialVideo = AfternoteMemorialVideoDto(videoUrl = "v.mp4", thumbnailUrl = "t.jpg"),
                    ),
            ).toDetailDomain()

        val memorial = (result.content as DetailContent.Memorial).memorial
        val media = memorial.media
        assertEquals("memorial.jpg", media.photoUrl)
        assertEquals("v.mp4", media.videoUrl)
        assertEquals("t.jpg", media.thumbnailUrl)
        assertEquals(1, memorial.songs.size)
        assertEquals("s", memorial.songs.single().title)
        assertEquals("a", memorial.songs.single().artist)
    }

    @Test
    fun `toDetailDomain - playlist의 공통 leaveMessage도 보존한다`() {
        val result =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                leaveMessage = listOf(LeaveMessageBlockDto(title = "가족에게", body = "잘 지내")),
                memorial = AfternotePlaylistDto(),
            ).toDetailDomain()

        assertEquals(
            listOf(LeaveMessageBlock(title = "가족에게", body = "잘 지내")),
            result.leaveMessageBlocks,
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

        val media = (result.content as DetailContent.Memorial).memorial.media
        assertNull(media.videoUrl)
        assertNull(media.thumbnailUrl)
    }

    @Test
    fun `toDetailDomain - playlist 타입에 playlist가 없으면 오류`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                AfternoteDetailDto(
                    afternoteId = 1L,
                    category = "PLAYLIST",
                    title = "t",
                ).toDetailDomain()
            }

        assertEquals("playlist is required for MEMORIAL detail", exception.message)
    }

    @Test
    fun `toDetailDomain - business와 estate도 각각 배타적인 content로 매핑`() {
        val business =
            AfternoteDetailDto(
                afternoteId = 1L,
                category = "BUSINESS",
                title = "회사 계정",
                credentials = AfternoteCredentialsDto(id = "user", password = "pw"),
            ).toDetailDomain()
        val estate =
            AfternoteDetailDto(
                afternoteId = 2L,
                category = "ESTATE",
                title = "부동산",
            ).toDetailDomain()

        assertTrue(business.content is DetailContent.Business)
        assertEquals(AfternoteType.BUSINESS, business.content.type)
        assertEquals(DetailContent.Estate, estate.content)
    }
}
