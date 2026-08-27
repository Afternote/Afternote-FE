package com.afternote.feature.afternote.presentation.shared.util

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 카드 아이콘이 서비스명 문자열이 아니라 서버 category 로 결정된다는 계약 (이슈 #622).
 *
 * 리소스 id 절대값이 아니라 "어느 해석 경로를 탔는가"를 비교해, R 값에 의존하지 않는다.
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
        AfternoteType.entries.forEach { type ->
            assertEquals(
                "$type 로 등록한 임의 서비스명은 $type 의 카테고리 아이콘을 받아야 한다",
                getIconResForType(type),
                getIconResForService(unlistedServiceName, type),
            )
        }
    }

    @Test
    fun `카탈로그에 있는 이름은 카테고리와 무관하게 서비스 아이콘을 쓴다`() {
        assertEquals(
            AfternoteService.INSTAGRAM.iconResId,
            getIconResForService(
                serviceName = AfternoteService.INSTAGRAM.displayKey,
                type = AfternoteType.GALLERY_AND_FILES,
            ),
        )
    }

    @Test
    fun `카탈로그에 없는 이름을 소셜로 단정하지 않는다`() {
        val onGallery = getIconResForService(unlistedServiceName, AfternoteType.GALLERY_AND_FILES)
        val onSocial = getIconResForService(unlistedServiceName, AfternoteType.SOCIAL_NETWORK)

        assertNotEquals(
            "같은 이름이라도 카테고리가 다르면 아이콘이 갈려야 한다 — 같으면 category 가 무시된 것",
            onSocial,
            onGallery,
        )
    }
}
