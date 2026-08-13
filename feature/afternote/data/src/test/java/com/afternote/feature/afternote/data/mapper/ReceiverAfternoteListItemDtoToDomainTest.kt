package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDto
import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ReceivedAfternoteDto.toDomain] / [toReceiverDomainList] 회귀 가드.
 * 수신자 목록 DTO→[com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItem] 매핑.
 * 서버 카테고리를 [AfternoteType] 으로 바꾸는 규칙과 null 가드(category·createdAt)를 검증한다.
 */
class ReceiverAfternoteListItemDtoToDomainTest {
    @Test
    fun `toDomain - 필드 매핑 + category 정규화 + 날짜 포맷`() {
        val result =
            ReceivedAfternoteDto(
                id = 9L,
                title = "사진첩",
                category = "GALLERY",
                createdAt = "2025-11-26T14:30:00",
            ).toDomain()

        assertEquals(9L, result.id)
        assertEquals("사진첩", result.title)
        assertEquals(AfternoteType.GALLERY_AND_FILES, result.type)
        assertEquals("2025.11.26", result.lastUpdatedAt)
    }

    @Test
    fun `toDomain - 변환은 대소문자 무시 + MUSIC PLAYLIST는 MEMORIAL`() {
        assertEquals(AfternoteType.SOCIAL_NETWORK, resp(category = "social").toDomain().type)
        assertEquals(AfternoteType.MEMORIAL, resp(category = "PLAYLIST").toDomain().type)
        assertEquals(AfternoteType.MEMORIAL, resp(category = "music").toDomain().type)
    }

    @Test
    fun `toDomain - 서버가 모르는 category 를 보내면 type null`() {
        assertNull(resp(category = "ESTATE").toDomain().type)
        assertNull(resp(category = "BUSINESS").toDomain().type)
    }

    @Test
    fun `toDomain - category null이면 type null`() {
        assertNull(resp(category = null).toDomain().type)
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
    ) = ReceivedAfternoteDto(
        id = id,
        title = title,
        category = category,
        createdAt = createdAt,
    )
}
