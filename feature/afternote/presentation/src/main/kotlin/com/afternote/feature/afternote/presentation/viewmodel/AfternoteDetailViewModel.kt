package com.afternote.feature.afternote.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.usecase.DeleteUseCase
import com.afternote.feature.afternote.domain.usecase.GetDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AfternoteDetailUiState {
    data object Loading : AfternoteDetailUiState

    data class Success(val detail: Detail) : AfternoteDetailUiState

    data class Error(val message: String) : AfternoteDetailUiState

    data object Deleted : AfternoteDetailUiState
}

@HiltViewModel
class AfternoteDetailViewModel
    @Inject
    constructor(
        private val getDetailUseCase: GetDetailUseCase,
        private val deleteUseCase: DeleteUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AfternoteDetailUiState>(AfternoteDetailUiState.Loading)
        val uiState: StateFlow<AfternoteDetailUiState> = _uiState.asStateFlow()

        fun loadDetail(id: Long) {
            _uiState.value = AfternoteDetailUiState.Loading
            viewModelScope.launch {
                getDetailUseCase(id).fold(
                    onSuccess = { detail ->
                        _uiState.value = AfternoteDetailUiState.Success(detail)
                    },
                    onFailure = { error ->
                        _uiState.value = AfternoteDetailUiState.Error(error.message ?: "Unknown error")
                    },
                )
            }
        }

        fun delete(id: Long) {
            viewModelScope.launch {
                deleteUseCase(id).fold(
                    onSuccess = {
                        _uiState.value = AfternoteDetailUiState.Deleted
                    },
                    onFailure = { error ->
                        _uiState.value = AfternoteDetailUiState.Error(error.message ?: "Unknown error")
                    },
                )
            }
        }
    }
