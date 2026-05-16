package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.compose.ui.text.style.TextAlign

data class TimeLetterWriteUiState(
    val recipientIds: List<Long> = emptyList(),
    val recipientNames: List<String> = emptyList(),
    val sendAt: String? = null,
    val sendTime: String? = null,
    val draftCount: Int = 0,
    val isSaving: Boolean = false,
    val textAlign: TextAlign = TextAlign.Start,
)

sealed interface TimeLetterWriteEvent {
    data object SavedAsDraft : TimeLetterWriteEvent

    data object Registered : TimeLetterWriteEvent

    data class Error(
        val message: String,
    ) : TimeLetterWriteEvent
}
