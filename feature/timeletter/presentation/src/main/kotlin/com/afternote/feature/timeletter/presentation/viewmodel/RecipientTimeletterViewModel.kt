package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipientTimeletterUiState {
    data object Loading : RecipientTimeletterUiState

    data class Success(
        val letters: ReceivedTimeLetterList,
    ) : RecipientTimeletterUiState

    data object Error : RecipientTimeletterUiState
}

@HiltViewModel
class RecipientTimeletterViewModel
    @Inject
    constructor(
        private val receiverTimeLetterRepository: ReceiverTimeLetterRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<RecipientTimeletterUiState>(RecipientTimeletterUiState.Loading)
        val uiState: StateFlow<RecipientTimeletterUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = RecipientTimeletterUiState.Loading
                runCatching { receiverTimeLetterRepository.getReceivedTimeLetters() }
                    .onSuccess { letters ->
                        _uiState.value = RecipientTimeletterUiState.Success(letters = letters)
                    }.onFailure {
                        _uiState.value = RecipientTimeletterUiState.Error
                    }
            }
        }
    }
