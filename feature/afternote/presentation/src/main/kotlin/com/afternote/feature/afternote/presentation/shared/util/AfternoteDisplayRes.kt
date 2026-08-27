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

/** Icon drawable res for an [AfternoteType]. */
fun getIconResForType(type: AfternoteType): Int =
    when (type) {
        AfternoteType.SOCIAL_NETWORK -> CoreUiR.drawable.core_ui_afternote_social_pattern
        AfternoteType.GALLERY_AND_FILES -> CoreUiR.drawable.core_ui_afternote_gallery_pattern
        AfternoteType.MEMORIAL -> CoreUiR.drawable.core_ui_afternote_memorial_guideline
        AfternoteType.BUSINESS, AfternoteType.ESTATE -> CoreUiR.drawable.core_ui_afternote_logo
    }

/**
 * Icon drawable res for a service title shown on a card.
 *
 * 시안([카테고리별 아이콘 보드 34:3342](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/%EC%95%A0%ED%94%84%ED%84%B0%EB%85%B8%ED%8A%B8--new-?node-id=34-3342))이
 * 아이콘을 정의한 서비스면 그 아이콘을, 정의하지 않은 이름(직접 입력 등)이면 [type] 의 카테고리 아이콘을 준다.
 * 이름만으로 카테고리를 추론하지 않는다 — 카테고리는 서버 category 에서 온 [type] 이 정본이다.
 */
fun getIconResForService(
    serviceName: String,
    type: AfternoteType,
): Int =
    AfternoteService.fromDisplayKeyOrNull(serviceName)?.iconResId
        ?: getIconResForType(type)
