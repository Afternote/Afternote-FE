package com.afternote.feature.receiver.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [ReceivedAfternoteDto.toDomain] / [toReceiverDomainList] 회귀 가드.
 * 수신자 목록 DTO→[com.afternote.feature.receiver.domain.model.AfterNoteListItem] 매핑.
 * 서버 카테고리를 [AfternoteType] 으로 바꾸는 규칙과 createdAt null 가드를 검증한다.
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
        assertEquals("사진첩", result.serviceName)
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
    fun `toDomain - 작성 미지원 category 도 알려진 도메인 type으로 보존`() {
        assertEquals(AfternoteType.ESTATE, resp(category = "ESTATE").toDomain().type)
        assertEquals(AfternoteType.BUSINESS, resp(category = "BUSINESS").toDomain().type)
    }

    @Test
    fun `toDomain - 대응하지 않는 type은 SOCIAL_NETWORK 기본값`() {
        assertEquals(AfternoteType.SOCIAL_NETWORK, resp(category = "UNKNOWN").toDomain().type)
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

    @Test
    fun `toDomainResult - 서버 totalCount와 목록을 함께 보존`() {
        val result =
            ReceivedAfternoteListDto(
                afternotes = listOf(resp(id = 3L), resp(id = 4L)),
                totalCount = 7,
            ).toDomainResult()

        assertEquals(7, result.totalCount)
        assertEquals(listOf(3L, 4L), result.items.map { it.id })
    }

    private fun resp(
        id: Long = 1L,
        title: String = "t",
        category: String = "SOCIAL",
        createdAt: String? = "2025-01-01T00:00:00",
    ) = ReceivedAfternoteDto(
        id = id,
        title = title,
        category = category,
        createdAt = createdAt,
    )
}
