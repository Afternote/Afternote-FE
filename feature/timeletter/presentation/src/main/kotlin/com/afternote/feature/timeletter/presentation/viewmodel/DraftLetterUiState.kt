package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.annotation.StringRes
import com.afternote.feature.timeletter.domain.model.TimeLetter

sealed interface DraftLetterUiState {
    data object Loading : DraftLetterUiState

    data class Success(
        val drafts: List<TimeLetter>,
        val receiverNameMap: Map<Long, String> = emptyMap(),
        val isEditMode: Boolean = false,
        val isDeleting: Boolean = false,
        val selectedIds: Set<Long> = emptySet(),
        @StringRes val messageRes: Int? = null,
    ) : DraftLetterUiState {
        val isDeleteSelectedEnabled: Boolean
            get() = selectedIds.isNotEmpty() && !isDeleting
    }

    data class Error(
        @StringRes val messageRes: Int,
    ) : DraftLetterUiState
}
