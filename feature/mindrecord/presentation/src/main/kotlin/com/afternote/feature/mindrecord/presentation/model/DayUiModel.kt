package com.afternote.feature.mindrecord.presentation.model

data class DayUiModel(
    val day: Int?,
    val state: DayState = DayState.NONE,
    val emotion: String? = null,
)
