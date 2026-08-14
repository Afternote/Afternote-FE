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

/**
 * 서버 `category` → [AfternoteType]. 대응이 없으면 [AfternoteType.SOCIAL_NETWORK] 로 폴백한다.
 *
 * 값 표는 [afternoteTypeFromServerCategory] 가 정본이다 — 여기에 다시 적으면 갈라진다.
 */
internal fun categoryToAfternoteType(category: String): AfternoteType =
    afternoteTypeFromServerCategory(category) ?: AfternoteType.SOCIAL_NETWORK
