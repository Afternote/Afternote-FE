package com.afternote.feature.mindrecord.presentation.model

import java.time.LocalDate

data class DailyQuestion(
    val id: Long = 0L,
    val title: String,
    val date: LocalDate,
    val content: String,
)
