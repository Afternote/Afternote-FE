package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ReceiverEditIntent : MviIntent {
    data class Update(
        val name: String,
        val relation: String,
        val phone: String,
        val email: String,
        val message: String,
    ) : ReceiverEditIntent

    data object ConsumeSuccess : ReceiverEditIntent
}

internal sealed interface ReceiverEditReducerEvent : ReducerEvent {
    data class Loaded(
        val receiver: com.afternote.core.model.user.ReceiverDetail,
    ) : ReceiverEditReducerEvent

    data class LoadFailed(
        val message: com.afternote.core.ui.UiText,
    ) : ReceiverEditReducerEvent

    data object Saving : ReceiverEditReducerEvent

    data object Saved : ReceiverEditReducerEvent

    data class SaveFailed(
        val message: com.afternote.core.ui.UiText,
    ) : ReceiverEditReducerEvent

    data object SuccessConsumed : ReceiverEditReducerEvent
}
