package com.afternote.feature.mindrecord.presentation.model

import java.time.LocalDate

data class DailyQuestion(
    val title: String,
    val date: LocalDate,
    val content: String,
)
