package com.afternote.feature.timeletter.presentation.viewmodel

data class TimeLetterWriteUiState(
    val recipientIds: List<Long> = emptyList(),
    val recipientNames: List<String> = emptyList(),
    val sendAt: String? = null,
    val draftCount: Int = 0,
    val isSaving: Boolean = false,
)

sealed interface TimeLetterWriteEvent {
    data object SavedAsDraft : TimeLetterWriteEvent
    data object Registered : TimeLetterWriteEvent
    data class Error(val message: String) : TimeLetterWriteEvent
}
