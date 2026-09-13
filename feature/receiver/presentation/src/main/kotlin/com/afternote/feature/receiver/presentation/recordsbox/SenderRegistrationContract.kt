package com.afternote.feature.receiver.presentation.recordsbox

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface SenderRegistrationIntent : MviIntent {
    data class Submit(
        val name: String,
    ) : SenderRegistrationIntent

    data object ConsumeRegistered : SenderRegistrationIntent
}

internal sealed interface SenderRegistrationReducerEvent : ReducerEvent {
    data object Registered : SenderRegistrationReducerEvent

    data object RegisteredConsumed : SenderRegistrationReducerEvent
}

internal fun reduceSenderRegistration(
    state: SenderRegistrationUiState,
    event: SenderRegistrationReducerEvent,
): SenderRegistrationUiState =
    when (event) {
        SenderRegistrationReducerEvent.Registered -> state.copy(isRegistered = true)
        SenderRegistrationReducerEvent.RegisteredConsumed -> state.copy(isRegistered = false)
    }
