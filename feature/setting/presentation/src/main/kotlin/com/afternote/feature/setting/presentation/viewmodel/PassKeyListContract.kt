package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.feature.setting.domain.Passkey

internal sealed interface PassKeyListIntent : MviIntent {
    data object Refresh : PassKeyListIntent
}

internal sealed interface PassKeyListReducerEvent : ReducerEvent {
    data object Loading : PassKeyListReducerEvent

    data class Loaded(
        val passkeys: List<Passkey>,
    ) : PassKeyListReducerEvent

    data object Failed : PassKeyListReducerEvent
}
