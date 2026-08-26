package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버 `category` 계약 회귀 가드.
 *
 * 목록 파라미터·생성 바디 모두 서버 `AfternoteCategoryType` 이라 어긋난 값은 400 이 된다.
 * 서버 값 표를 코드와 분리해 두어, 한쪽만 고치면 깨진다 — 서버 변경 자체를 감지하지는 못한다.
 */
class AfternoteCategoryCodecTest {
    /**
     * 2026-08-13 기준 서버 `AfternoteCategoryType` 전량 (Afternote-BE `78ee857`).
     *
     * `BUSINESS` 가 이 표에서 빠져 있던 동안 사업자 애프터노트가 비즈니스 탭에서 사라지고
     * 전체 탭에는 소셜로 앉아 있었다 (#1048).
     */
    private val serverEnumValues = setOf("SOCIAL", "BUSINESS", "GALLERY", "PLAYLIST")

    @Test
    fun `보낼 수 있는 값의 집합이 서버 enum 과 일치한다`() {
        val sendable = AfternoteType.entries.mapNotNull { it.toServerCategory() }.toSet()

        assertEquals(serverEnumValues, sendable)
    }

    @Test
    fun `사업자는 서버가 아는 종류다 - 보내고 받는 값이 BUSINESS 로 맞물린다`() {
        assertEquals("BUSINESS", AfternoteType.BUSINESS.toServerCategory())
        assertEquals(AfternoteType.BUSINESS, afternoteTypeFromServerCategory("BUSINESS"))
    }

    @Test
    fun `서버가 정의하지 않은 종류는 보낼 값이 없다`() {
        assertNull("ESTATE 는 아직 서버 enum 에 없다 (#491)", AfternoteType.ESTATE.toServerCategory())
    }

    @Test
    fun `추억 노트의 서버 값은 PLAYLIST 다 - MEMORIAL 을 보내면 서버가 거절한다`() {
        assertEquals("PLAYLIST", AfternoteType.MEMORIAL.toServerCategory())
        assertNull(afternoteTypeFromServerCategory("MEMORIAL"))
    }

    @Test
    fun `보낼 수 있는 종류는 왕복한다`() {
        AfternoteType.entries
            .filter { it.toServerCategory() != null }
            .forEach { type ->
                assertEquals(type, afternoteTypeFromServerCategory(type.toServerCategory()!!))
            }
    }

    @Test
    fun `수신은 대소문자를 가리지 않는다`() {
        assertEquals(AfternoteType.SOCIAL_NETWORK, afternoteTypeFromServerCategory("social"))
        assertEquals(AfternoteType.GALLERY_AND_FILES, afternoteTypeFromServerCategory("Gallery"))
        assertEquals(AfternoteType.BUSINESS, afternoteTypeFromServerCategory("business"))
    }

    @Test
    fun `MUSIC 은 추억 노트의 옛 서버 값으로 함께 받는다`() {
        assertEquals(AfternoteType.MEMORIAL, afternoteTypeFromServerCategory("MUSIC"))
        assertTrue("옛 값은 받기만 한다", AfternoteType.entries.none { it.toServerCategory() == "MUSIC" })
    }

    @Test
    fun `대응이 없는 서버 값은 null 을 준다 - 특정 종류로 메우지 않는다`() {
        assertNull(afternoteTypeFromServerCategory("ESTATE"))
        assertNull(afternoteTypeFromServerCategory("WHAT_IS_THIS"))
        assertNull(afternoteTypeFromServerCategory(""))
    }

    @Test
    fun `서버 값은 종류마다 고유하다`() {
        val sendable = AfternoteType.entries.mapNotNull { it.toServerCategory() }

        assertEquals("같은 서버 값을 두 종류가 나눠 가지면 응답 변환이 갈린다", sendable.size, sendable.toSet().size)
    }
}
