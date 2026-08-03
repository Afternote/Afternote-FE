package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType

internal fun formatDateFromServer(serverDateTime: String): String =
    try {
        // Server returns "2025-11-26T14:30:00" -> display "2025.11.26"
        val datePart = serverDateTime.substringBefore('T')
        datePart.replace('-', '.')
    } catch (_: Exception) {
        serverDateTime
    }

internal fun categoryToAfternoteType(category: String): AfternoteType =
    when (category.uppercase()) {
        "SOCIAL" -> AfternoteType.SOCIAL_NETWORK
        "BUSINESS" -> AfternoteType.BUSINESS
        "GALLERY" -> AfternoteType.GALLERY_AND_FILES
        "ESTATE" -> AfternoteType.ESTATE
        "MUSIC", "PLAYLIST" -> AfternoteType.MEMORIAL
        else -> AfternoteType.SOCIAL_NETWORK
    }
