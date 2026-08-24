package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType

/**
 * 서버 `category`(백엔드 `AfternoteCategoryType`) 값의 정본 — 도메인·화면은 [AfternoteType] 만 다룬다.
 */
private val serverCategoryByType: Map<AfternoteType, String> =
    mapOf(
        AfternoteType.SOCIAL_NETWORK to "SOCIAL",
        AfternoteType.GALLERY_AND_FILES to "GALLERY",
        AfternoteType.MEMORIAL to "PLAYLIST",
    )

/** [AfternoteType.MEMORIAL] 의 옛 서버 값. 받기만 하고 보내지는 않는다. */
private const val LEGACY_MEMORIAL_CATEGORY = "MUSIC"

internal fun AfternoteType.toServerCategory(): String? = serverCategoryByType[this]

fun afternoteTypeFromServerCategory(value: String): AfternoteType? {
    val normalized = value.uppercase()
    if (normalized == LEGACY_MEMORIAL_CATEGORY) return AfternoteType.MEMORIAL
    return serverCategoryByType.entries.firstOrNull { it.value == normalized }?.key
}

/** 서버 `category` → [AfternoteType]. 대응이 없으면 [AfternoteType.SOCIAL_NETWORK] 로 폴백한다. */
fun categoryToAfternoteType(category: String): AfternoteType = afternoteTypeFromServerCategory(category) ?: AfternoteType.SOCIAL_NETWORK
