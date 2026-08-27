package com.afternote.feature.setting.presentation.viewmodel

data class ReceiverRegisterUiState(
    val isLoading: Boolean = false,
    val error: ReceiverRegisterError? = null,
)

sealed interface ReceiverRegisterError {
    data class ServerMessage(
        val value: String,
    ) : ReceiverRegisterError

    data object Generic : ReceiverRegisterError
}

sealed interface ReceiverRegisterEvent {
    data object RegisterSuccess : ReceiverRegisterEvent
}
