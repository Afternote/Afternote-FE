package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteServiceType

internal fun formatDateFromServer(serverDateTime: String): String =
    try {
        // Server returns "2025-11-26T14:30:00" -> display "2025.11.26"
        val datePart = serverDateTime.substringBefore('T')
        datePart.replace('-', '.')
    } catch (_: Exception) {
        serverDateTime
    }

internal fun categoryToServiceType(category: String): AfternoteServiceType =
    when (category.uppercase()) {
        "SOCIAL" -> AfternoteServiceType.SOCIAL_NETWORK
        "GALLERY" -> AfternoteServiceType.GALLERY_AND_FILES
        "MUSIC", "PLAYLIST" -> AfternoteServiceType.MEMORIAL
        else -> AfternoteServiceType.SOCIAL_NETWORK
    }
