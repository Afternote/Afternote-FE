package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType

/** 서버에 보낼 수 있는 `category` 값. 미지원 종류(BUSINESS: Afternote-BE#110, ESTATE: #491)는 제외한다. */
private val serverCategoryByType: Map<AfternoteType, String> =
    mapOf(
        AfternoteType.SOCIAL_NETWORK to "SOCIAL",
        AfternoteType.GALLERY_AND_FILES to "GALLERY",
        AfternoteType.MEMORIAL to "PLAYLIST",
    )

/** [AfternoteType.MEMORIAL] 의 옛 서버 값. 받기만 하고 보내지는 않는다. */
private const val LEGACY_MEMORIAL_CATEGORY = "MUSIC"

/**
 * 응답에서 인식할 `category` 값. 현재 outbound 미지원 종류도 이미 앱의 도메인 축에 존재하므로 타입을 유실하지 않는다.
 */
private val afternoteTypeByIncomingCategory: Map<String, AfternoteType> =
    serverCategoryByType.entries.associate { (type, category) -> category to type } +
        mapOf(
            "BUSINESS" to AfternoteType.BUSINESS,
            "ESTATE" to AfternoteType.ESTATE,
            LEGACY_MEMORIAL_CATEGORY to AfternoteType.MEMORIAL,
        )

internal fun AfternoteType.toServerCategory(): String? = serverCategoryByType[this]

fun afternoteTypeFromServerCategory(value: String): AfternoteType? = afternoteTypeByIncomingCategory[value.uppercase()]
