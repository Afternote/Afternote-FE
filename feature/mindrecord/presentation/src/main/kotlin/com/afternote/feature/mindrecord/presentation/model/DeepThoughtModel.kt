package com.afternote.feature.mindrecord.presentation.model

import java.time.LocalDate

data class DeepThoughtModel(
    val id: Long = 0L,
    val title: String,
    val content: String,
    val date: LocalDate,
    val tag: List<Tag>,
    val category: String = "",
    val isDraft: Boolean = false,
)
