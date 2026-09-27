package com.afternote.feature.setting.presentation.profile

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ProfileEditIntent : MviIntent {
    data class SelectPhoto(
        val uri: String,
    ) : ProfileEditIntent

    data class UpdateProfile(
        val name: String,
        val phone: String,
    ) : ProfileEditIntent

    /** 화면이 처리한 [event] 를 되돌려준다. 그사이 다른 신호가 올라왔으면 그 신호는 남는다. */
    data class ConsumeEvent(
        val event: ProfileEditEvent,
    ) : ProfileEditIntent
}

internal sealed interface ProfileEditReducerEvent : ReducerEvent {
    data class Loaded(
        val name: String,
        val phone: String,
        val email: String,
        val profileImageUrl: String?,
    ) : ProfileEditReducerEvent

    data class PhotoSelected(
        val uri: String,
    ) : ProfileEditReducerEvent

    data object LoadFailed : ProfileEditReducerEvent

    data object Updating : ProfileEditReducerEvent

    data object UpdateSucceeded : ProfileEditReducerEvent

    data object UpdateFailed : ProfileEditReducerEvent

    data class EventConsumed(
        val event: ProfileEditEvent,
    ) : ProfileEditReducerEvent
}
