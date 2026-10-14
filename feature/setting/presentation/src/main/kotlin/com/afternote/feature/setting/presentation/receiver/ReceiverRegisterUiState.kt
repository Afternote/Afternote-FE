package com.afternote.feature.setting.presentation.receiver

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal data class ReceiverRegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val pendingEvent: ReceiverRegisterEvent? = null,
) : UiState

internal sealed interface ReceiverRegisterIntent : MviIntent {
    data class Register(
        val name: String,
        val relation: String,
        val phone: String?,
        val email: String,
        val message: String?,
    ) : ReceiverRegisterIntent

    data class ConsumeEvent(
        val event: ReceiverRegisterEvent,
    ) : ReceiverRegisterIntent
}

internal sealed interface ReceiverRegisterReducerEvent : ReducerEvent {
    data object RegisterStarted : ReceiverRegisterReducerEvent

    data object RegisterSucceeded : ReceiverRegisterReducerEvent

    data class RegisterFailed(
        val message: UiText,
    ) : ReceiverRegisterReducerEvent

    data class EventConsumed(
        val event: ReceiverRegisterEvent,
    ) : ReceiverRegisterReducerEvent
}

internal sealed interface ReceiverRegisterEvent {
    data object RegisterSuccess : ReceiverRegisterEvent
}
