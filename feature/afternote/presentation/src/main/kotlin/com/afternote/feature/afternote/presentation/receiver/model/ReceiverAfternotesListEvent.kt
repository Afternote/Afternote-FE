package com.afternote.feature.afternote.presentation.receiver.model

sealed interface ReceiverAfternotesListEvent {
    data class Load(val authCode: String) : ReceiverAfternotesListEvent
    data object Retry : ReceiverAfternotesListEvent
    data object ErrorConsumed : ReceiverAfternotesListEvent
}
