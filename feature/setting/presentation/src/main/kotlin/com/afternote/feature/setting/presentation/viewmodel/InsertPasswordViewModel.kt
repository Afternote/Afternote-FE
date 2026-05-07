package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class InsertPasswordViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(InsertPasswordUiState())
        val uiState: StateFlow<InsertPasswordUiState> = _uiState.asStateFlow()

        fun onDigitInput(digit: String) {
            _uiState.update { state ->
                if (state.password.length < 4) {
                    state.copy(password = state.password + digit)
                } else {
                    state
                }
            }
        }

        fun onDelete() {
            _uiState.update { state ->
                state.copy(password = state.password.dropLast(1))
            }
        }
    }
