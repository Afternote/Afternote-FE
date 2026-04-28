package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.feature.timeletter.domain.DraftLetter

data class DraftLetterUiState(
    val drafts: List<DraftLetter> = emptyList(),
    val isEditMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
)
