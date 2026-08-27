package com.afternote.feature.afternote.data.mapper

fun formatDateFromServer(serverDateTime: String): String =
    try {
        // Server returns "2025-11-26T14:30:00" -> display "2025.11.26"
        val datePart = serverDateTime.substringBefore('T')
        datePart.replace('-', '.')
    } catch (_: Exception) {
        serverDateTime
    }
