package com.afternote.feature.afternote.presentation

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal data class AfternoteHostUiState(
    val isPasskeyRegistered: Boolean? = null,
) : UiState

internal sealed interface AfternoteHostIntent : MviIntent {
    data object ObserveProfile : AfternoteHostIntent

    data object StopObservingProfile : AfternoteHostIntent
}

internal sealed interface AfternoteHostReducerEvent : ReducerEvent {
    data class PasskeyRegistrationChanged(
        val registered: Boolean,
    ) : AfternoteHostReducerEvent
}
