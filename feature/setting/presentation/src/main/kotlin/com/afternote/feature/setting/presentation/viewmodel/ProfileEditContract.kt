package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ProfileEditIntent : MviIntent {
    data class UpdateProfile(
        val name: String,
        val phone: String,
    ) : ProfileEditIntent

    data class ConsumeEvent(
        val event: ProfileEditEvent,
    ) : ProfileEditIntent
}

internal sealed interface ProfileEditReducerEvent : ReducerEvent {
    data class Loaded(
        val name: String,
        val phone: String,
        val email: String,
    ) : ProfileEditReducerEvent

    data object LoadFailed : ProfileEditReducerEvent

    data object Updating : ProfileEditReducerEvent

    data class UpdateFinished(
        val event: ProfileEditEvent,
    ) : ProfileEditReducerEvent

    data class EventConsumed(
        val event: ProfileEditEvent,
    ) : ProfileEditReducerEvent
}
