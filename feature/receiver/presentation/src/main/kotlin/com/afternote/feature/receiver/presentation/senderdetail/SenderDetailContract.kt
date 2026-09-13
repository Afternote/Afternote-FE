package com.afternote.feature.receiver.presentation.senderdetail

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface SenderDetailIntent : MviIntent {
    data object RefreshOnReturn : SenderDetailIntent

    data object OpenReceiverHome : SenderDetailIntent

    data object ConsumeOpenReceiverHome : SenderDetailIntent
}

internal sealed interface SenderDetailReducerEvent : ReducerEvent {
    data object SenderNotFound : SenderDetailReducerEvent

    data object Loading : SenderDetailReducerEvent

    data class Loaded(
        val resolved: SenderDetailUiState,
        val keepsStateOnFailure: Boolean,
    ) : SenderDetailReducerEvent

    data object ReceiverHomeRequested : SenderDetailReducerEvent

    data object ReceiverHomeConsumed : SenderDetailReducerEvent
}

internal fun reduceSenderDetail(
    state: SenderDetailUiState,
    event: SenderDetailReducerEvent,
): SenderDetailUiState =
    when (event) {
        SenderDetailReducerEvent.SenderNotFound -> {
            SenderDetailUiState.SenderNotFound
        }

        SenderDetailReducerEvent.Loading -> {
            SenderDetailUiState.Loading
        }

        is SenderDetailReducerEvent.Loaded -> {
            when {
                event.keepsStateOnFailure &&
                    event.resolved is SenderDetailUiState.StatusLoadFailed &&
                    state is SenderDetailUiState.Success -> {
                    state
                }

                event.resolved is SenderDetailUiState.Success && state is SenderDetailUiState.Success -> {
                    event.resolved.copy(
                        shouldOpenReceiverHome = state.shouldOpenReceiverHome,
                    )
                }

                else -> {
                    event.resolved
                }
            }
        }

        SenderDetailReducerEvent.ReceiverHomeRequested -> {
            if (state is SenderDetailUiState.Success) state.copy(shouldOpenReceiverHome = true) else state
        }

        SenderDetailReducerEvent.ReceiverHomeConsumed -> {
            if (state is SenderDetailUiState.Success) state.copy(shouldOpenReceiverHome = false) else state
        }
    }
