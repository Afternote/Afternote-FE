package com.afternote.feature.timeletter.domain

@JvmInline
value class OpenDate(
    val value: String,
)

data class LetterSchedule(
    val recipientName: String,
    val openDate: OpenDate,
)
