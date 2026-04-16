package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DraftLetterViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(DraftLetterUiState())
    val uiState: StateFlow<DraftLetterUiState> = _uiState.asStateFlow()

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditMode = !it.isEditMode, selectedIds = emptySet()) }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val updated = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = updated)
        }
    }

    fun deleteSelected() {
        _uiState.update { state ->
            state.copy(
                drafts = state.drafts.filter { it.id !in state.selectedIds },
                selectedIds = emptySet(),
                isEditMode = false,
            )
        }
    }
}
