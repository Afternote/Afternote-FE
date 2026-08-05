package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.feature.timeletter.domain.model.TimeLetterList

sealed interface TimeletterUiState {
    data object Loading : TimeletterUiState

    data class Success(
        val letters: TimeLetterList,
        val receiverNameMap: Map<Long, String>,
        val selectedFilterReceiverIds: Set<Long> = emptySet(),
        val isDeleting: Boolean = false,
        val showDeleteFailure: Boolean = false,
        val errorMessage: String? = null,
    ) : TimeletterUiState

    data class Error(
        val message: String,
    ) : TimeletterUiState
}
