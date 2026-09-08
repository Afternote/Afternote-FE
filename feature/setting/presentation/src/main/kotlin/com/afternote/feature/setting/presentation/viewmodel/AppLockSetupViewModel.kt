package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
internal class AppLockSetupViewModel
    @Inject
    constructor() :
    MviViewModel<AppLockSetupIntent, AppLockSetupUiState, AppLockSetupReducerEvent>(AppLockSetupUiState()) {
        override fun onIntent(intent: AppLockSetupIntent) {
            when (intent) {
                is AppLockSetupIntent.DigitInput -> dispatch(AppLockSetupReducerEvent.DigitEntered(intent.digit))
                AppLockSetupIntent.Delete -> dispatch(AppLockSetupReducerEvent.Deleted)
                AppLockSetupIntent.ResetPin -> dispatch(AppLockSetupReducerEvent.PinReset)
            }
        }

        override fun reduce(
            state: AppLockSetupUiState,
            event: AppLockSetupReducerEvent,
        ): AppLockSetupUiState =
            when (event) {
                is AppLockSetupReducerEvent.DigitEntered -> {
                    if (state.pin.length < 4) {
                        val newPin = state.pin + event.digit
                        state.copy(pin = newPin, isComplete = newPin.length == 4)
                    } else {
                        state
                    }
                }

                AppLockSetupReducerEvent.Deleted -> {
                    state.copy(pin = state.pin.dropLast(1), isComplete = false)
                }

                AppLockSetupReducerEvent.PinReset -> {
                    AppLockSetupUiState()
                }
            }
    }
