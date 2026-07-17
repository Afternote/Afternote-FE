package com.afternote.feature.mindrecord.presentation.model

import java.time.LocalDate

data class DailyDiary(
    val id: Long = 0L,
    val title: String,
    val date: LocalDate,
    val content: String,
    val emotion: String? = null,
    val imageUrl: String? = null,
)
