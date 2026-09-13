package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal sealed interface ReceiverListIntent : MviIntent {
    data object ObservationStarted : ReceiverListIntent

    data object ObservationStopped : ReceiverListIntent
}

internal data class ReceiverListUiState(
    val receivers: List<ReceiverListItem> = emptyList(),
) : UiState

internal sealed interface ReceiverListReducerEvent : ReducerEvent {
    data class ReceiversChanged(
        val receivers: List<ReceiverListItem>,
    ) : ReceiverListReducerEvent
}
