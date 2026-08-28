package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.feature.timeletter.domain.model.TimeLetter

sealed interface DraftLetterUiState {
    data object Loading : DraftLetterUiState

    data class Success(
        val drafts: List<TimeLetter>,
        val isEditMode: Boolean = false,
        val selectedIds: Set<Long> = emptySet(),
        val isDeleting: Boolean = false,
        val errorMessage: String? = null,
    ) : DraftLetterUiState

    data class Error(
        val message: String,
    ) : DraftLetterUiState
}
