package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType

/**
 * 서버 `category`(백엔드 `AfternoteCategoryType`) 값의 정본 — 도메인·화면은 [AfternoteType] 만 다룬다.
 *
 * `ESTATE`(#491)만 아직 서버 enum 에 없어 여기 없고, 보내면 400 이다.
 * `BUSINESS` 는 Afternote-BE `78ee857`(2026-08-13) 부터 `SOCIAL` 과 동일 계약으로 정식 값이다.
 */
private val serverCategoryByType: Map<AfternoteType, String> =
    mapOf(
        AfternoteType.SOCIAL_NETWORK to "SOCIAL",
        AfternoteType.BUSINESS to "BUSINESS",
        AfternoteType.GALLERY_AND_FILES to "GALLERY",
        AfternoteType.MEMORIAL to "PLAYLIST",
    )

/** [AfternoteType.MEMORIAL] 의 옛 서버 값. 받기만 하고 보내지는 않는다. */
private const val LEGACY_MEMORIAL_CATEGORY = "MUSIC"

internal fun AfternoteType.toServerCategory(): String? = serverCategoryByType[this]

/**
 * 서버 `category` → [AfternoteType]. 표에 없는 값은 `null` 이다 — 임의의 종류로 메우지 않는다.
 *
 * 호출부는 이 `null` 을 «해석 실패» 로 다뤄야 한다(목록은 항목 기각, 단건은 실패).
 * 특정 종류로 폴백하면 서버가 새 값을 추가한 순간 조용한 오표시가 된다 — #1048 이 그 사고였다.
 */
fun afternoteTypeFromServerCategory(value: String): AfternoteType? {
    val normalized = value.uppercase()
    if (normalized == LEGACY_MEMORIAL_CATEGORY) return AfternoteType.MEMORIAL
    return serverCategoryByType.entries.firstOrNull { it.value == normalized }?.key
}
