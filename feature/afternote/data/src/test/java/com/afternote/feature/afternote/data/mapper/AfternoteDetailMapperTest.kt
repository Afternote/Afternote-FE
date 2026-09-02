package com.afternote.feature.afternote.data.mapper

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
 * [AfternoteDetailDto.toDomain] 회귀 가드 (작성자 상세).
 * 핵심 경계: 공통 필드와 타입별 [DetailContent] 분리, 부분·부재 credentials 의 빈 값 강등,
 * receivers null→emptyList, receiver 필드 null→"", processingMethods null→emptyList,
 * memorialVideo null→video/thumbnail null.
 */
class AfternoteDetailMapperTest {
    @Test
    fun `toDomain - gallery 최소 응답은 컬렉션이 비어 있다`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
                updatedAt = UPDATED_AT,
            ).toDomain()

        assertEquals(1L, result.id)
        val content = result.content as DetailContent.Gallery
        assertEquals(AfternoteType.GALLERY_AND_FILES, content.type)
        assertTrue(result.receivers.isEmpty())
        assertTrue(content.processingMethods.isEmpty())
    }

    @Test
    fun `toDomain - 사업자 상세는 BUSINESS 로 올라온다 - 소셜로 둔갑하지 않는다`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "BUSINESS",
                title = "t",
                updatedAt = UPDATED_AT,
            ).toDomain()

        assertTrue("사업자는 Business 내용으로 올라와야 한다", result.content is DetailContent.Business)
    }

    @Test
    fun `toDomain - 해석할 수 없는 category 는 실패다 - 임의의 종류로 메우지 않는다`() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                AfternoteDetailDto(
                    isDraft = false,
                    receivers = emptyList(),
                    afternoteId = 42L,
                    category = "???",
                    title = "t",
                    updatedAt = UPDATED_AT,
                ).toDomain()
            }

        assertTrue(
            "진단에 식별자와 원본 값이 남아야 한다",
            thrown.message!!.contains("42") && thrown.message!!.contains("???"),
        )
    }

    @Test
    fun `toDomain - timestamps 포맷`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
                updatedAt = "2025-12-01T09:00:00",
            ).toDomain()

        assertEquals("2025.12.01", result.timestamps.updatedAt)
    }

    @Test
    fun `toDomain - receiver의 null 필드는 빈 문자열`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
                updatedAt = UPDATED_AT,
                receivers = listOf(AfternoteDetailReceiverDto(receiverId = 5L)),
            ).toDomain()

        val receiver = result.receivers.single()
        assertTrue(result.content is DetailContent.Gallery)
        assertEquals(5L, receiver.receiverId)
        assertEquals("", receiver.name)
        assertEquals("", receiver.relation)
    }

    /**
     * DTO 는 방어적으로 receiverId 가 nullable 이지만 도메인은 non-null 이므로, 경계인 이 매퍼가 걸러야 한다.
     */
    @Test
    fun `toDomain - receiverId 없는 항목은 도메인으로 올리지 않는다`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                afternoteId = 1L,
                category = "GALLERY",
                title = "t",
                updatedAt = UPDATED_AT,
                receivers =
                    listOf(
                        AfternoteDetailReceiverDto(receiverId = null, name = "식별자없음"),
                        AfternoteDetailReceiverDto(receiverId = 9L, name = "김수신"),
                    ),
            ).toDomain()

        assertEquals(listOf(9L), result.receivers.map { it.receiverId })
    }

    @Test
    fun `toDomain - credentials 매핑`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                updatedAt = UPDATED_AT,
                credentials = AfternoteCredentialsDto(id = "user", password = "pw"),
            ).toDomain()

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("user", credentials.id)
        assertEquals("pw", credentials.password)
    }

    @Test
    fun `toDomain - credentials가 아예 없으면 빈 값으로 낮춘다`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                updatedAt = UPDATED_AT,
            ).toDomain()

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("", credentials.id)
        assertEquals("", credentials.password)
    }

    @Test
    fun `toDomain - credentials id만 없으면 id를 빈 값으로 낮춘다`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                updatedAt = UPDATED_AT,
                credentials = AfternoteCredentialsDto(id = null, password = "pw"),
            ).toDomain()

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("", credentials.id)
        assertEquals("pw", credentials.password)
    }

    @Test
    fun `toDomain - credentials password만 없으면 password를 빈 값으로 낮춘다`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                updatedAt = UPDATED_AT,
                credentials = AfternoteCredentialsDto(id = "user", password = null),
            ).toDomain()

        val credentials = (result.content as DetailContent.SocialNetwork).credentials
        assertEquals("user", credentials.id)
        assertEquals("", credentials.password)
    }

    @Test
    fun `toDomain - playlist 미디어·곡 매핑`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                updatedAt = UPDATED_AT,
                memorial =
                    AfternotePlaylistDto(
                        memorialPhotoUrl = "memorial.jpg",
                        songs = listOf(AfternoteSongDto(title = "s", artist = "a")),
                        memorialVideo = AfternoteMemorialVideoDto(videoUrl = "v.mp4", thumbnailUrl = "t.jpg"),
                    ),
            ).toDomain()

        val memorial = result.content as DetailContent.Memorial
        val media = memorial.media
        assertEquals("memorial.jpg", media.photoUrl)
        assertEquals("v.mp4", media.videoUrl)
        assertEquals("t.jpg", media.thumbnailUrl)
        assertEquals(1, memorial.songs.size)
        assertEquals("s", memorial.songs.single().title)
        assertEquals("a", memorial.songs.single().artist)
    }

    @Test
    fun `toDomain - playlist의 공통 leaveMessage도 보존한다`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                updatedAt = UPDATED_AT,
                leaveMessage = listOf(LeaveMessageBlockDto(title = "가족에게", body = "잘 지내")),
                memorial = AfternotePlaylistDto(songs = emptyList()),
            ).toDomain()

        assertEquals(
            listOf(LeaveMessageBlock(title = "가족에게", body = "잘 지내")),
            result.leaveMessageBlocks,
        )
    }

    @Test
    fun `toDomain - memorialVideo null이면 video thumbnail null`() {
        val result =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                updatedAt = UPDATED_AT,
                memorial = AfternotePlaylistDto(songs = emptyList(), memorialVideo = null),
            ).toDomain()

        val media = (result.content as DetailContent.Memorial).media
        assertNull(media.videoUrl)
        assertNull(media.thumbnailUrl)
    }

    @Test
    fun `toDomain - 발행 playlist 에 playlist 가 없으면 계약 위반이라 실패한다`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                AfternoteDetailDto(
                    isDraft = false,
                    receivers = emptyList(),
                    afternoteId = 1L,
                    category = "PLAYLIST",
                    title = "t",
                    updatedAt = UPDATED_AT,
                ).toDomain()
            }

        assertEquals("발행 상세에 playlist 가 없다: afternoteId=1", exception.message)
    }

    @Test
    fun `toDomain - 사업자 상세도 credentials 를 Business 내용에 싣는다`() {
        val business =
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "BUSINESS",
                title = "회사 계정",
                updatedAt = UPDATED_AT,
                credentials = AfternoteCredentialsDto(id = "user", password = "pw"),
            ).toDomain()

        val content = business.content as DetailContent.Business
        assertEquals(AfternoteType.BUSINESS, content.type)
        assertEquals("user", content.credentials.id)
        assertEquals("pw", content.credentials.password)
    }

    /** `ESTATE`(#491)는 아직 서버 enum 에 없어 응답으로 올라올 수 없다 — 왔다면 해석 실패다. */
    @Test
    fun `toDomain - ESTATE 는 서버가 보낼 수 없는 값이라 해석 실패다`() {
        assertThrows(IllegalArgumentException::class.java) {
            AfternoteDetailDto(
                isDraft = false,
                receivers = emptyList(),
                afternoteId = 2L,
                category = "ESTATE",
                title = "부동산",
                updatedAt = UPDATED_AT,
            ).toDomain()
        }
    }

    private companion object {
        const val UPDATED_AT = "2025-12-01T09:00:00"
    }
}
