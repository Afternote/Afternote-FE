package com.afternote.feature.afternote.presentation.shared.util

import androidx.annotation.StringRes
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService
import com.afternote.core.ui.R as CoreUiR

/**
 * 종류 필터 탭 순서. `null` 은 "전체" 탭이다.
 * 재산 처리는 서버 category enum 미지원(Afternote-BE#491)이라 빠져 있다.
 */
val TYPE_FILTER_TABS: List<AfternoteType?> =
    listOf(
        null,
        AfternoteType.SOCIAL_NETWORK,
        AfternoteType.BUSINESS,
        AfternoteType.GALLERY_AND_FILES,
        AfternoteType.MEMORIAL,
    )

/** 종류 탭 라벨. `null` 은 "전체" 탭이다. */
@StringRes
fun typeLabelResFor(type: AfternoteType?): Int =
    when (type) {
        null -> R.string.afternote_category_all
        AfternoteType.SOCIAL_NETWORK -> R.string.afternote_category_social_network
        AfternoteType.BUSINESS -> R.string.afternote_category_business
        AfternoteType.GALLERY_AND_FILES -> R.string.afternote_category_gallery_and_files
        AfternoteType.ESTATE -> R.string.afternote_category_estate
        AfternoteType.MEMORIAL -> R.string.afternote_category_memorial
    }

/**
 * 서비스 이름과 함께 표시할 아이콘 리소스.
 *
 * 서비스명 카탈로그에 등록된 이름이면 해당 서비스 아이콘을 사용한다.
 * 카탈로그에 없는 이름은 대체 아이콘이 확정될 때까지 기본 로고를 사용한다(#753).
 */
fun getIconResForService(serviceName: String): Int =
    AfternoteService.fromDisplayKeyOrNull(serviceName)?.iconResId
        ?: CoreUiR.drawable.core_ui_afternote_logo
