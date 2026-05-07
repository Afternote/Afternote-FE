package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class AppLockSetupViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(AppLockSetupUiState())
        val uiState: StateFlow<AppLockSetupUiState> = _uiState.asStateFlow()

        fun onDigitInput(digit: String) {
            _uiState.update { state ->
                if (state.pin.length < 4) {
                    val newPin = state.pin + digit
                    state.copy(pin = newPin, isComplete = newPin.length == 4)
                } else {
                    state
                }
            }
        }

        fun onDelete() {
            _uiState.update { state ->
                state.copy(pin = state.pin.dropLast(1), isComplete = false)
            }
        }

        fun resetPin() {
            _uiState.update { AppLockSetupUiState() }
        }
    }
