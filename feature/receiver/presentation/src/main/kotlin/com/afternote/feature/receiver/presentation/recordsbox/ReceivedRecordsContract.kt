package com.afternote.feature.receiver.presentation.recordsbox

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal sealed interface ReceivedRecordsIntent : MviIntent

internal data class ReceivedRecordsUiState(
    val senders: List<SenderEntry> = emptyList(),
) : UiState

internal sealed interface ReceivedRecordsReducerEvent : ReducerEvent {
    data class SendersChanged(
        val senders: List<SenderEntry>,
    ) : ReceivedRecordsReducerEvent
}

internal fun reduceReceivedRecords(
    state: ReceivedRecordsUiState,
    event: ReceivedRecordsReducerEvent,
): ReceivedRecordsUiState =
    when (event) {
        is ReceivedRecordsReducerEvent.SendersChanged -> state.copy(senders = event.senders)
    }
