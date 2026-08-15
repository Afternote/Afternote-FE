package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<DraftLetterUiState>(DraftLetterUiState.Loading)
        val uiState: StateFlow<DraftLetterUiState> = _uiState.asStateFlow()

        init {
            loadDrafts()
        }

        fun loadDrafts() {
            viewModelScope.launch {
                _uiState.value = DraftLetterUiState.Loading
                try {
                    val result = timeLetterRepository.getTemporaryTimeLetters()
                    val receiverNameMap =
                        runCatching { userRepository.getReceivers() }
                            .getOrDefault(emptyList())
                            .associate { receiver -> receiver.receiverId to receiver.name }
                    _uiState.value =
                        DraftLetterUiState.Success(
                            drafts = result.timeLetters,
                            receiverNameMap = receiverNameMap,
                        )
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: Exception) {
                    _uiState.value = DraftLetterUiState.Error(R.string.timeletter_draft_load_error)
                }
            }
        }

        fun toggleEditMode() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            _uiState.value = current.copy(isEditMode = !current.isEditMode, selectedIds = emptySet())
        }

        fun toggleSelection(id: Long) {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            if (id !in current.drafts.map { it.id }) return
            val updated = if (id in current.selectedIds) current.selectedIds - id else current.selectedIds + id
            _uiState.value = current.copy(selectedIds = updated)
        }

        fun deleteSelected() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            if (current.selectedIds.isEmpty()) return
            viewModelScope.launch {
                try {
                    timeLetterRepository.deleteTimeLetters(current.selectedIds.toList())
                    _uiState.value =
                        current.copy(
                            drafts = current.drafts.filter { it.id !in current.selectedIds },
                            selectedIds = emptySet(),
                            isEditMode = false,
                        )
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: Exception) {
                    _uiState.value = current.copy(messageRes = R.string.timeletter_draft_delete_error)
                }
            }
        }

        fun deleteAll() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            viewModelScope.launch {
                try {
                    timeLetterRepository.deleteAllTemporary()
                    _uiState.value = DraftLetterUiState.Success(drafts = emptyList())
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (_: Exception) {
                    _uiState.value = current.copy(messageRes = R.string.timeletter_draft_delete_error)
                }
            }
        }

        fun onMessageShown() {
            val current = _uiState.value as? DraftLetterUiState.Success ?: return
            _uiState.value = current.copy(messageRes = null)
        }
    }
