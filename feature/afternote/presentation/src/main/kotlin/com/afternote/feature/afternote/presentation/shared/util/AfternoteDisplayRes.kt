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
 * 카탈로그 밖 서비스에 표시할 카테고리 아이콘.
 *
 * 소셜([4163:19696](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4163-19696)),
 * 비즈니스([4163:19699](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4163-19699)),
 * 갤러리([4163:19661](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/?node-id=4163-19661))는
 * Figma 최종 보드의 카테고리 아이콘을 사용한다.
 * 재산 처리는 별도 아이콘이 없어 문서 성격이 같은 비즈니스 아이콘을 공유한다.
 */
private fun getIconResForType(type: AfternoteType): Int =
    when (type) {
        AfternoteType.SOCIAL_NETWORK -> CoreUiR.drawable.core_ui_afternote_social_pattern
        AfternoteType.BUSINESS, AfternoteType.ESTATE -> CoreUiR.drawable.core_ui_afternote_business_pattern
        AfternoteType.GALLERY_AND_FILES -> CoreUiR.drawable.core_ui_afternote_gallery_category_pattern
        AfternoteType.MEMORIAL -> CoreUiR.drawable.core_ui_afternote_memorial_guideline
    }

/**
 * 서비스 이름과 함께 표시할 아이콘 리소스.
 *
 * 시안([카테고리별 아이콘 보드 4327:64346](https://www.figma.com/design/UP9ZR186jHvRBicjA2SOea/%EC%95%A0%ED%94%84%ED%84%B0%EB%85%B8%ED%8A%B8--new-?node-id=4327-64346))에
 * 등록된 서비스명이면 해당 서비스 아이콘을 사용한다.
 * 등록되지 않은 이름(직접 입력 등)이면 서버 category에서 온 [type]의 카테고리 아이콘을 사용한다(#753).
 * 이름만으로 카테고리를 추론하지 않는다 — 카테고리는 서버 category에서 온 [type]이 정본이다.
 */
fun getIconResForService(
    serviceName: String,
    type: AfternoteType,
): Int =
    AfternoteService.fromDisplayKeyOrNull(serviceName)?.iconResId
        ?: getIconResForType(type)
