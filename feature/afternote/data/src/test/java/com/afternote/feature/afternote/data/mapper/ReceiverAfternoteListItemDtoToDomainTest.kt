package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ReceivedAfternoteResponse.toDomain] / [toReceiverDomainList] 회귀 가드.
 * 수신자 목록 DTO→[com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto] 매핑.
 * 서버 카테고리(SOCIAL/GALLERY/PLAYLIST/MUSIC)를 presentation typeKey로 정규화하는 규칙과
 * null 가드(category·createdAt)를 검증한다.
 */
class ReceiverAfternoteListItemDtoToDomainTest {
    @Test
    fun `toDomain - 필드 매핑 + category 정규화 + 날짜 포맷`() {
        val result =
            ReceivedAfternoteResponse(
                id = 9L,
                title = "사진첩",
                category = "GALLERY",
                createdAt = "2025-11-26T14:30:00",
            ).toDomain()

        assertEquals(9L, result.id)
        assertEquals("사진첩", result.title)
        assertEquals("GALLERY_AND_FILES", result.sourceType)
        assertEquals("2025.11.26", result.lastUpdatedAt)
    }

    @Test
    fun `toDomain - category 정규화는 대소문자 무시 + MUSIC PLAYLIST는 MEMORIAL`() {
        assertEquals("SOCIAL_NETWORK", resp(category = "social").toDomain().sourceType)
        assertEquals("MEMORIAL", resp(category = "PLAYLIST").toDomain().sourceType)
        assertEquals("MEMORIAL", resp(category = "music").toDomain().sourceType)
    }

    @Test
    fun `toDomain - 정규화 규칙에 없는 category는 원본 유지`() {
        assertEquals("ESTATE", resp(category = "ESTATE").toDomain().sourceType)
    }

    @Test
    fun `toDomain - category null이면 sourceType null`() {
        assertNull(resp(category = null).toDomain().sourceType)
    }

    @Test
    fun `toDomain - createdAt null이면 lastUpdatedAt null`() {
        assertNull(resp(createdAt = null).toDomain().lastUpdatedAt)
    }

    @Test
    fun `toReceiverDomainList - 각 항목을 순서대로 매핑`() {
        val list = listOf(resp(id = 1L), resp(id = 2L)).toReceiverDomainList()
        assertEquals(listOf(1L, 2L), list.map { it.id })
    }

    private fun resp(
        id: Long = 1L,
        title: String? = "t",
        category: String? = "SOCIAL",
        createdAt: String? = "2025-01-01T00:00:00",
    ) = ReceivedAfternoteResponse(
        id = id,
        title = title,
        category = category,
        createdAt = createdAt,
    )
}
