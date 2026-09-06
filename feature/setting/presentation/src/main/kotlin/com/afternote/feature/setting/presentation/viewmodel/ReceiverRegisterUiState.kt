package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.UiText

data class ReceiverRegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)

sealed interface ReceiverRegisterEvent {
    data object RegisterSuccess : ReceiverRegisterEvent
}
