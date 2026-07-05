package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipientTimeLetterDetailUiState {
    data object Loading : RecipientTimeLetterDetailUiState

    data class Success(
        val letter: ReceivedTimeLetter,
    ) : RecipientTimeLetterDetailUiState

    data object Error : RecipientTimeLetterDetailUiState
}

@HiltViewModel
class RecipientTimeLetterDetailViewModel
    @Inject
    constructor(
        private val receiverTimeLetterRepository: ReceiverTimeLetterRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val timeLetterReceiverId: Long =
            savedStateHandle.toRoute<TimeLetterRoute.TimeLetterRecipientDetailRoute>().timeLetterReceiverId

        private val _uiState = MutableStateFlow<RecipientTimeLetterDetailUiState>(RecipientTimeLetterDetailUiState.Loading)
        val uiState: StateFlow<RecipientTimeLetterDetailUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = RecipientTimeLetterDetailUiState.Loading
                runCatching { receiverTimeLetterRepository.getReceivedTimeLetterDetail(timeLetterReceiverId) }
                    .onSuccess { letter ->
                        _uiState.value = RecipientTimeLetterDetailUiState.Success(letter = letter)
                    }.onFailure {
                        _uiState.value = RecipientTimeLetterDetailUiState.Error
                    }
            }
        }
    }
