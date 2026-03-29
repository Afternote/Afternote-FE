package com.afternote.feature.mindrecord.presentation.model

import java.time.LocalDate

data class DeepThoughtModel(
    val title: String,
    val content: String,
    val date: LocalDate,
    val tag: List<Tag>,
)
