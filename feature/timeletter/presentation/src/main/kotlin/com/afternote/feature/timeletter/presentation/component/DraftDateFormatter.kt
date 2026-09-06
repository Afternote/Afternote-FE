package com.afternote.feature.timeletter.presentation.component

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val draftDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.", Locale.ROOT)

internal fun formatDraftSendAt(sendAt: String?): String {
    if (sendAt.isNullOrBlank()) return "–"

    return try {
        LocalDate.parse(sendAt.take(10)).format(draftDateFormatter)
    } catch (_: DateTimeParseException) {
        "–"
    }
}
