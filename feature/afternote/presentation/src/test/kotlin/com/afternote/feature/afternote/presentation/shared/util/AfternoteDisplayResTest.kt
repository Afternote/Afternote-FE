package com.afternote.feature.afternote.presentation.shared.util

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import com.afternote.core.ui.R as CoreUiR

/**
 * 카드 아이콘은 알려진 서비스명이 우선이고, 카탈로그 밖 이름만 서버 category를 따른다는 계약 (이슈 #753).
 *
 * 카테고리 fallback은 Figma 최종 보드의 40dp 그라데이션 아이콘을 사용한다.
 */
class AfternoteDisplayResTest {
    private val unlistedServiceName = "직접 입력한 서비스"

    @Test
    fun `홈 카테고리 필터는 비즈니스를 노출하고 서버 미지원 재산 처리는 제외한다`() {
        assertEquals(
            listOf(
                null,
                AfternoteType.SOCIAL_NETWORK,
                AfternoteType.BUSINESS,
                AfternoteType.GALLERY_AND_FILES,
                AfternoteType.MEMORIAL,
            ),
            TYPE_FILTER_TABS,
        )
    }

    @Test
    fun `카탈로그에 없는 이름은 그 항목의 카테고리 아이콘으로 떨어진다`() {
        val expectedByType =
            mapOf(
                AfternoteType.SOCIAL_NETWORK to CoreUiR.drawable.core_ui_afternote_social_pattern,
                AfternoteType.BUSINESS to CoreUiR.drawable.core_ui_afternote_business_pattern,
                AfternoteType.GALLERY_AND_FILES to CoreUiR.drawable.core_ui_afternote_gallery_category_pattern,
                AfternoteType.MEMORIAL to CoreUiR.drawable.core_ui_afternote_memorial_guideline,
                AfternoteType.ESTATE to CoreUiR.drawable.core_ui_afternote_business_pattern,
            )

        expectedByType.forEach { (type, expectedIconRes) ->
            assertEquals(
                "$type 로 등록한 임의 서비스명은 $type 의 카테고리 아이콘을 받아야 한다",
                expectedIconRes,
                getIconResForService(unlistedServiceName, type),
            )
        }
    }

    @Test
    fun `카탈로그에 있는 이름은 전달된 카테고리와 무관하게 해당 서비스 아이콘을 쓴다`() {
        AfternoteService.entries.forEach { service ->
            AfternoteType.entries.forEach { type ->
                assertEquals(
                    "${service.displayKey}는 $type 에서도 ${service.name}의 서비스 아이콘을 받아야 한다",
                    service.iconResId,
                    getIconResForService(service.displayKey, type),
                )
            }
        }
    }

    @Test
    fun `알려진 갤러리 서비스 아이콘과 미등록 갤러리 카테고리 아이콘은 다르다`() {
        val knownGallery =
            getIconResForService(
                AfternoteService.GALLERY.displayKey,
                AfternoteType.GALLERY_AND_FILES,
            )
        val unlistedGallery =
            getIconResForService(
                unlistedServiceName,
                AfternoteType.GALLERY_AND_FILES,
            )

        assertEquals(AfternoteService.GALLERY.iconResId, knownGallery)
        assertEquals(CoreUiR.drawable.core_ui_afternote_gallery_category_pattern, unlistedGallery)
        assertNotEquals(knownGallery, unlistedGallery)
    }
}
