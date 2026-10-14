package com.afternote.feature.afternote.presentation.home

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.afternote.domain.AfternoteType

internal data class AfternoteHomeUiState(
    val selectedType: AfternoteType? = null,
) : UiState

internal sealed interface AfternoteHomeIntent : MviIntent {
    data class SelectType(
        val type: AfternoteType?,
    ) : AfternoteHomeIntent

    data class ListLoadFailed(
        val throwable: Throwable,
    ) : AfternoteHomeIntent

    data object ListLoadSucceeded : AfternoteHomeIntent
}

internal sealed interface AfternoteHomeReducerEvent : ReducerEvent {
    data class TypeSelected(
        val type: AfternoteType?,
    ) : AfternoteHomeReducerEvent
}
