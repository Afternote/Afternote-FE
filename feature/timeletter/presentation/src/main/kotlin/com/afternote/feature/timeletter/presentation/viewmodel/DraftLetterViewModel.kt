package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftLetterViewModel
    @Inject
    constructor(
        private val timeLetterRepository: TimeLetterRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<DraftLetterUiState>(DraftLetterUiState.Loading)
        val uiState: StateFlow<DraftLetterUiState> = _uiState.asStateFlow()

        init {
            loadDrafts()
        }

        fun loadDrafts() {
            viewModelScope.launch {
                _uiState.value = DraftLetterUiState.Loading
                runCatching { timeLetterRepository.getTemporaryTimeLetters() }
                    .onSuccess { result ->
                        _uiState.value = DraftLetterUiState.Success(drafts = result.timeLetters)
                    }.onFailure {
                        _uiState.value = DraftLetterUiState.Error("임시저장 레터를 불러올 수 없습니다.")
                    }
            }
        }

        fun toggleEditMode() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            if (current.isDeleting) return
            _uiState.value = current.copy(isEditMode = !current.isEditMode, selectedIds = emptySet())
        }

        fun toggleSelection(id: Long) {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            if (current.isDeleting) return
            val updated = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
            _uiState.value = current.copy(selectedIds = updated)
        }

        fun deleteSelected() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            if (current.selectedIds.isEmpty() || current.isDeleting) return
            _uiState.value = current.copy(isDeleting = true, errorMessage = null)
            viewModelScope.launch {
                runCatching {
                    timeLetterRepository.deleteTimeLetters(current.selectedIds.toList())
                }.onSuccess {
                    _uiState.value =
                        current.copy(
                            drafts = current.drafts.filter { it.id !in current.selectedIds },
                            selectedIds = emptySet(),
                            isEditMode = false,
                            isDeleting = false,
                        )
                }.onFailure {
                    _uiState.value = current.copy(isDeleting = false, errorMessage = "임시저장 레터를 삭제할 수 없습니다.")
                }
            }
        }

        fun deleteAll() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            if (current.isDeleting) return
            _uiState.value = current.copy(isDeleting = true, errorMessage = null)
            viewModelScope.launch {
                runCatching {
                    timeLetterRepository.deleteAllTemporary()
                }.onSuccess {
                    _uiState.value = DraftLetterUiState.Success(drafts = emptyList())
                }.onFailure {
                    _uiState.value = current.copy(isDeleting = false, errorMessage = "임시저장 레터를 삭제할 수 없습니다.")
                }
            }
        }

        fun consumeErrorMessage() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            _uiState.value = current.copy(errorMessage = null)
        }
    }
