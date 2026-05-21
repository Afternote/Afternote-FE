package com.afternote.feature.setting.presentation.viewmodel

data class ReceiverRegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ReceiverRegisterEvent {
    data object RegisterSuccess : ReceiverRegisterEvent
}
