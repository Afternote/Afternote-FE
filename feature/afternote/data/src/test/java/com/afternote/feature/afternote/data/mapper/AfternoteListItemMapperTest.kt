package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteListItemDto
import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [toDomainList] 회귀 가드.
 * 작성자 목록 DTO→[com.afternote.feature.afternote.domain.model.author.ListItem] 매핑 +
 * 공유 헬퍼([formatDateFromServer]·[afternoteTypeFromServerCategory])의 경계 동작을 검증한다.
 *
 * 항목 매퍼를 따로 노출하지 않으므로 단건도 이 소비 경로로 확인한다 — 실제로 도는 경로 그대로다.
 */
class AfternoteListItemMapperTest {
    @Test
    fun `필드와 Long id를 그대로 매핑`() {
        val result =
            listOf(
                AfternoteListItemDto(
                    afternoteId = 7L,
                    title = "은행 계정",
                    category = "SOCIAL",
                    createdAt = "2025-11-26T14:30:00",
                ),
            ).toDomainList().single()

        assertEquals(7L, result.id)
        assertEquals("은행 계정", result.serviceName)
        assertEquals("2025.11.26", result.date)
        assertEquals(AfternoteType.SOCIAL_NETWORK, result.type)
    }

    @Test
    fun `category 매핑은 대소문자 무시 + MUSIC PLAYLIST는 MEMORIAL`() {
        assertEquals(AfternoteType.MEMORIAL, typeOf(category = "music"))
        assertEquals(AfternoteType.MEMORIAL, typeOf(category = "PLAYLIST"))
        assertEquals(AfternoteType.GALLERY_AND_FILES, typeOf(category = "gallery"))
    }

    @Test
    fun `사업자 항목은 BUSINESS 로 올라온다 - 소셜로 둔갑하지 않는다`() {
        assertEquals(AfternoteType.BUSINESS, typeOf(category = "BUSINESS"))
    }

    @Test
    fun `알 수 없는 category 는 항목을 기각한다 - 임의의 종류로 메우지 않는다`() {
        assertTrue(listOf(item(category = "???")).toDomainList().isEmpty())
        assertTrue(listOf(item(category = "ESTATE")).toDomainList().isEmpty())
    }

    @Test
    fun `createdAt에 T가 없어도 dash를 dot으로 치환`() {
        assertEquals("2025.11.26", listOf(item(createdAt = "2025-11-26")).toDomainList().single().date)
    }

    @Test
    fun `빈 리스트는 빈 리스트`() {
        assertEquals(emptyList<Any>(), emptyList<AfternoteListItemDto>().toDomainList())
    }

    @Test
    fun `각 항목을 순서대로 매핑`() {
        val list = listOf(item(afternoteId = 1L), item(afternoteId = 2L)).toDomainList()
        assertEquals(listOf(1L, 2L), list.map { it.id })
    }

    @Test
    fun `기각된 항목만 빠지고 나머지 페이지는 살아남는다`() {
        val list =
            listOf(
                item(afternoteId = 1L, category = "SOCIAL"),
                item(afternoteId = 2L, category = "???"),
                item(afternoteId = 3L, category = "BUSINESS"),
            ).toDomainList()

        assertEquals("실패의 폭은 그 항목 하나다", listOf(1L, 3L), list.map { it.id })
    }

    private fun typeOf(category: String) = listOf(item(category = category)).toDomainList().single().type

    private fun item(
        afternoteId: Long = 1L,
        title: String = "t",
        category: String = "SOCIAL",
        createdAt: String = "2025-01-01T00:00:00",
    ) = AfternoteListItemDto(
        afternoteId = afternoteId,
        title = title,
        category = category,
        createdAt = createdAt,
    )
}
