package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal sealed interface ReceiverListIntent : MviIntent {
    data object ObservationStarted : ReceiverListIntent

    data object Retry : ReceiverListIntent

    data object ObservationStopped : ReceiverListIntent
}

internal data class ReceiverListUiState(
    val receivers: List<ReceiverListItem> = emptyList(),
    val loadState: ReceiverListLoadState = ReceiverListLoadState.Loading,
) : UiState

internal enum class ReceiverListLoadState { Loading, Ready, InitialFailure, RefreshFailure }

internal sealed interface ReceiverListReducerEvent : ReducerEvent {
    data object RetryStarted : ReceiverListReducerEvent

    data class ResultChanged(
        val result: ReceiverListState,
    ) : ReceiverListReducerEvent
}
