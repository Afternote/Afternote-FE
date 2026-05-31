package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteListItem
import com.afternote.feature.afternote.domain.AfternoteServiceType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [AfternoteListItem.toDomain] / [toDomainList] 회귀 가드.
 * 작성자 목록 DTO→[com.afternote.feature.afternote.domain.model.author.ListItem] 매핑 +
 * 공유 헬퍼([formatDateFromServer]·[categoryToServiceType])의 경계 동작을 toDomain 경유로 검증.
 */
class AfternoteListItemDtoToDomainTest {
    @Test
    fun `toDomain - 필드 매핑 + id Long을 String으로`() {
        val result =
            AfternoteListItem(
                afternoteId = 7L,
                title = "은행 계정",
                category = "SOCIAL",
                createdAt = "2025-11-26T14:30:00",
            ).toDomain()

        assertEquals("7", result.id)
        assertEquals("은행 계정", result.serviceName)
        assertEquals("2025.11.26", result.date)
        assertEquals(AfternoteServiceType.SOCIAL_NETWORK, result.type)
    }

    @Test
    fun `toDomain - category 매핑은 대소문자 무시 + MUSIC PLAYLIST는 MEMORIAL`() {
        assertEquals(AfternoteServiceType.MEMORIAL, item(category = "music").toDomain().type)
        assertEquals(AfternoteServiceType.MEMORIAL, item(category = "PLAYLIST").toDomain().type)
        assertEquals(AfternoteServiceType.GALLERY_AND_FILES, item(category = "gallery").toDomain().type)
    }

    @Test
    fun `toDomain - 알 수 없는 category는 SOCIAL_NETWORK 기본값`() {
        assertEquals(AfternoteServiceType.SOCIAL_NETWORK, item(category = "???").toDomain().type)
    }

    @Test
    fun `toDomain - createdAt에 T가 없어도 dash를 dot으로 치환`() {
        assertEquals("2025.11.26", item(createdAt = "2025-11-26").toDomain().date)
    }

    @Test
    fun `toDomainList - 빈 리스트는 빈 리스트`() {
        assertEquals(emptyList<Any>(), emptyList<AfternoteListItem>().toDomainList())
    }

    @Test
    fun `toDomainList - 각 항목을 순서대로 매핑`() {
        val list = listOf(item(afternoteId = 1L), item(afternoteId = 2L)).toDomainList()
        assertEquals(listOf("1", "2"), list.map { it.id })
    }

    private fun item(
        afternoteId: Long = 1L,
        title: String = "t",
        category: String = "SOCIAL",
        createdAt: String = "2025-01-01T00:00:00",
    ) = AfternoteListItem(
        afternoteId = afternoteId,
        title = title,
        category = category,
        createdAt = createdAt,
    )
}
