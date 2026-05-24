package com.afternote.feature.setting.domain

import java.time.LocalDate

data class Notice(
    val date: LocalDate,
    val title: String,
    val content: String,
)
