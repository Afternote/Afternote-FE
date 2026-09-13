package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface AppLockSetupIntent : MviIntent {
    data class DigitInput(
        val digit: String,
    ) : AppLockSetupIntent

    data object Delete : AppLockSetupIntent

    data object ResetPin : AppLockSetupIntent
}

internal sealed interface AppLockSetupReducerEvent : ReducerEvent {
    data class DigitEntered(
        val digit: String,
    ) : AppLockSetupReducerEvent

    data object Deleted : AppLockSetupReducerEvent

    data object PinReset : AppLockSetupReducerEvent
}
