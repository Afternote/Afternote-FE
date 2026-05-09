package com.afternote.feature.afternote.presentation.receiver.list

sealed interface ReceiverAfternotesListEvent {
    data object Load : ReceiverAfternotesListEvent

    data object Retry : ReceiverAfternotesListEvent

    data object ErrorConsumed : ReceiverAfternotesListEvent
}
