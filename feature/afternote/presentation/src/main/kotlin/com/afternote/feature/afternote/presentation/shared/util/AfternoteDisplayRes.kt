package com.afternote.feature.afternote.presentation.shared.util

import androidx.annotation.StringRes
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.model.AfternoteService

/** Label and icon drawable resource IDs for an afternote type key. */
data class AfternoteRes(
    val stringResId: Int,
    val drawableResId: Int,
)

/**
 * 종류 필터 탭 순서. `null` 은 "전체" 탭이다.
 * 비즈니스·재산 처리는 목록 조회 미지원(Afternote-BE#110, #491)이라 빠져 있다.
 */
val CATEGORY_FILTER_TABS: List<AfternoteType?> =
    listOf(
        null,
        AfternoteType.SOCIAL_NETWORK,
        AfternoteType.GALLERY_AND_FILES,
        AfternoteType.MEMORIAL,
    )

/** 종류 탭 라벨. `null` 은 "전체" 탭이다. */
@StringRes
fun categoryLabelResFor(type: AfternoteType?): Int =
    when (type) {
        null -> R.string.afternote_category_all
        AfternoteType.SOCIAL_NETWORK -> R.string.afternote_category_social_network
        AfternoteType.BUSINESS -> R.string.afternote_category_business
        AfternoteType.GALLERY_AND_FILES -> R.string.afternote_category_gallery_and_files
        AfternoteType.ESTATE -> R.string.afternote_category_estate
        AfternoteType.MEMORIAL -> R.string.afternote_category_memorial
    }

/**
 * @param typeKey Writer: e.g. SOCIAL_NETWORK, GALLERY_AND_FILES, MEMORIAL. Receiver: e.g. INSTAGRAM, GALLERY, GUIDE, NAVER_MAIL.
 */
fun getAfternoteDisplayRes(typeKey: String): AfternoteRes {
    AfternoteService.fromTypeKeyOrNull(typeKey)?.let { svc ->
        return AfternoteRes(stringResId = svc.stringResId, drawableResId = svc.iconResId)
    }
    val drawableResId =
        AfternoteService.fromDisplayKeyOrNull(typeKey)?.iconResId
            ?: R.drawable.feature_afternote_img_logo
    return AfternoteRes(stringResId = R.string.afternote_category_social_network, drawableResId = drawableResId)
}

/**
 * Icon drawable res for an [AfternoteType]. Same mapping as [getAfternoteDisplayRes]; use when you have [AfternoteType].
 */
fun getIconResForType(type: AfternoteType): Int =
    AfternoteService.fromTypeKeyOrNull(type.name)?.iconResId
        ?: R.drawable.feature_afternote_img_logo

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

/**
 * API typeKey(예: "INSTAGRAM") → 화면 표시명(예: "인스타그램") 변환.
 * 매핑이 없으면 typeKey를 그대로 반환합니다.
 */
fun getServiceNameForTypeKey(typeKey: String): String = AfternoteService.fromTypeKeyOrNull(typeKey)?.displayKey ?: typeKey
