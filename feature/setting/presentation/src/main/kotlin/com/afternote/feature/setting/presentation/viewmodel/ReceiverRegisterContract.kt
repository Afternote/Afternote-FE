package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ReceiverRegisterIntent : MviIntent {
    data class Register(
        val name: String,
        val relation: String,
        val phone: String?,
        val email: String,
        val message: String?,
    ) : ReceiverRegisterIntent

    data object ConsumeSuccess : ReceiverRegisterIntent
}

internal sealed interface ReceiverRegisterReducerEvent : ReducerEvent {
    data object Registering : ReceiverRegisterReducerEvent

    data object Registered : ReceiverRegisterReducerEvent

    data class Failed(
        val message: com.afternote.core.ui.UiText,
    ) : ReceiverRegisterReducerEvent

    data object SuccessConsumed : ReceiverRegisterReducerEvent
}
