package com.afternote.feature.afternote.presentation.receiver.afternotelist

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.afternote.domain.AfternoteType

internal data class ReceiverAfternoteHomeUiState(
    val selectedTab: AfternoteType? = null,
) : UiState

internal sealed interface ReceiverAfternoteHomeIntent : MviIntent {
    data class SelectType(
        val type: AfternoteType?,
    ) : ReceiverAfternoteHomeIntent
}

internal sealed interface ReceiverAfternoteHomeReducerEvent : ReducerEvent {
    data class TypeSelected(
        val type: AfternoteType?,
    ) : ReceiverAfternoteHomeReducerEvent
}
