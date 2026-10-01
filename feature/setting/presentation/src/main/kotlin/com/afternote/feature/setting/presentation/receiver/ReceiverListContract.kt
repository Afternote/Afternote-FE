package com.afternote.feature.setting.presentation.receiver

import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal data class ReceiverListUiState(
    val receivers: List<ReceiverListItem> = emptyList(),
    val loadState: ReceiverListLoadState = ReceiverListLoadState.Loading,
    val sessionId: String? = null,
) : UiState

internal sealed interface ReceiverListIntent : MviIntent {
    data object ObservationStarted : ReceiverListIntent

    data object ObservationStopped : ReceiverListIntent

    data object Retry : ReceiverListIntent
}

internal sealed interface ReceiverListReducerEvent : ReducerEvent {
    data object RetryStarted : ReceiverListReducerEvent

    data class RepositoryStateChanged(
        val state: ReceiverListState,
    ) : ReceiverListReducerEvent
}

internal enum class ReceiverListLoadState {
    Loading,
    Ready,
    Failure,
    RefreshFailure,
}
