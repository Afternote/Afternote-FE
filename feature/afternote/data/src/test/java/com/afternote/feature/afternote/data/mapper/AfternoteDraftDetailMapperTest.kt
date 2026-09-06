package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCredentialsDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideoDto
import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AfternoteDetailDto.toDraftDomain] 회귀 가드 (임시저장 상세 = 이어쓰기 프리필).
 *
 * 핵심 경계: 임시저장은 서버가 카테고리별 필수값을 검증하지 않으므로 종류별 값이 통째로 빠질 수 있고,
 * 그 «아직 없음» 을 던지지 않고 그대로 전달해야 이어쓰기가 열린다. 발행 상세의 엄격함은
 * [AfternoteDetailMapperTest] 가 따로 지킨다.
 */
class AfternoteDraftDetailMapperTest {
    @Test
    fun `toDraftDomain - 곡을 안 담은 임시저장 playlist 는 곡 0개와 빈 미디어가 된다`() {
        val result =
            AfternoteDetailDto(
                isDraft = true,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                updatedAt = UPDATED_AT,
            ).toDraftDomain()

        assertEquals(AfternoteType.MEMORIAL, result.type)
        assertTrue(result.songs.isEmpty())
        assertNull(result.media.photoUrl)
        assertNull(result.media.videoUrl)
        assertNull(result.media.thumbnailUrl)
    }

    @Test
    fun `toDraftDomain - 담은 곡과 미디어는 그대로 실린다`() {
        val result =
            AfternoteDetailDto(
                isDraft = true,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "PLAYLIST",
                title = "t",
                updatedAt = UPDATED_AT,
                memorial =
                    AfternotePlaylistDto(
                        memorialPhotoUrl = "photo",
                        songs = listOf(AfternoteSongDto(title = "곡", artist = "가수", coverUrl = "cover")),
                        memorialVideo = AfternoteMemorialVideoDto(videoUrl = "video", thumbnailUrl = "thumb"),
                    ),
            ).toDraftDomain()

        assertEquals(listOf("곡"), result.songs.map { it.title })
        assertEquals("photo", result.media.photoUrl)
        assertEquals("video", result.media.videoUrl)
        assertEquals("thumb", result.media.thumbnailUrl)
    }

    @Test
    fun `toDraftDomain - 계정 정보를 아직 안 쓴 임시저장은 credentials 가 null 이다`() {
        val result =
            AfternoteDetailDto(
                isDraft = true,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                updatedAt = UPDATED_AT,
            ).toDraftDomain()

        assertEquals(AfternoteType.SOCIAL_NETWORK, result.type)
        assertNull(result.credentials)
    }

    @Test
    fun `toDraftDomain - 한쪽만 채운 계정 정보는 그 한쪽만 살린다`() {
        val result =
            AfternoteDetailDto(
                isDraft = true,
                receivers = emptyList(),
                afternoteId = 1L,
                category = "SOCIAL",
                title = "t",
                updatedAt = UPDATED_AT,
                credentials = AfternoteCredentialsDto(id = "user", password = null),
            ).toDraftDomain()

        assertEquals("user", result.credentials?.id)
        assertEquals("", result.credentials?.password)
    }

    @Test
    fun `toDraftDomain - 해석할 수 없는 종류는 폼을 못 고르므로 실패한다`() {
        val exception =
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                AfternoteDetailDto(
                    isDraft = true,
                    receivers = emptyList(),
                    afternoteId = 7L,
                    category = "UNKNOWN",
                    title = "t",
                    updatedAt = UPDATED_AT,
                ).toDraftDomain()
            }

        assertTrue(exception.message!!.contains("afternoteId=7"))
    }

    private companion object {
        const val UPDATED_AT = "2026-08-07T06:21:14.553567"
    }
}
