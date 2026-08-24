package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType

private val serverCategoryByType: Map<AfternoteType, String> =
    mapOf(
        AfternoteType.SOCIAL_NETWORK to "SOCIAL",
        AfternoteType.GALLERY_AND_FILES to "GALLERY",
        AfternoteType.MEMORIAL to "PLAYLIST",
    )

/** [AfternoteType.MEMORIAL] 의 옛 서버 값. 받기만 하고 보내지는 않는다. */
private const val LEGACY_MEMORIAL_CATEGORY = "MUSIC"

private val afternoteTypeByIncomingCategory: Map<String, AfternoteType> =
    serverCategoryByType.entries.associate { (type, category) -> category to type } +
        mapOf(
            "BUSINESS" to AfternoteType.BUSINESS,
            "ESTATE" to AfternoteType.ESTATE,
            LEGACY_MEMORIAL_CATEGORY to AfternoteType.MEMORIAL,
        )

internal fun AfternoteType.toServerCategory(): String? = serverCategoryByType[this]

fun afternoteTypeFromServerCategory(value: String): AfternoteType? = afternoteTypeByIncomingCategory[value.uppercase()]
