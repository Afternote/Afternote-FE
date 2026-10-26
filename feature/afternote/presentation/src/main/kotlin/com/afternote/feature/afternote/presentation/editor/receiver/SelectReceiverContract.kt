package com.afternote.feature.afternote.presentation.editor.receiver

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface SelectReceiverIntent : MviIntent {
    data class ApplyPreselection(
        val receiverIds: List<Long>,
    ) : SelectReceiverIntent

    data object Refresh : SelectReceiverIntent

    data class ToggleReceiver(
        val receiverId: Long,
    ) : SelectReceiverIntent
}

internal sealed interface SelectReceiverReducerEvent : ReducerEvent {
    data class PreselectionApplied(
        val receiverIds: List<Long>,
    ) : SelectReceiverReducerEvent

    data object Loading : SelectReceiverReducerEvent

    data class ReceiversLoaded(
        val receivers: List<AfternoteEditorReceiver>,
    ) : SelectReceiverReducerEvent

    data object LoadFailed : SelectReceiverReducerEvent

    data class ReceiverToggled(
        val receiverId: Long,
    ) : SelectReceiverReducerEvent
}
