package com.afternote.feature.afternote.presentation.shared.util

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService
import org.junit.Assert.assertEquals
import org.junit.Test
import com.afternote.core.ui.R as CoreUiR

/**
 * 카드 아이콘을 서비스명만으로 결정한다는 계약 (이슈 #753).
 *
 * 카탈로그 조회와 대체 경로가 선택해야 할 리소스 ID를 직접 고정한다.
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
    fun `카탈로그에 없는 이름은 기본 로고로 떨어진다`() {
        assertEquals(
            CoreUiR.drawable.core_ui_afternote_logo,
            getIconResForService(unlistedServiceName),
        )
    }

    @Test
    fun `카탈로그에 있는 이름은 해당 서비스 아이콘을 쓴다`() {
        AfternoteService.entries.forEach { service ->
            assertEquals(
                "${service.displayKey}는 ${service.name}의 서비스 아이콘을 받아야 한다",
                service.iconResId,
                getIconResForService(service.displayKey),
            )
        }
    }
}
