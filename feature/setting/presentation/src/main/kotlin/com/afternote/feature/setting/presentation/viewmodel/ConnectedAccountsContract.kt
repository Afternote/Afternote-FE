package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ConnectedAccountsIntent : MviIntent {
    data class Toggle(
        val provider: String,
        val enabled: Boolean,
    ) : ConnectedAccountsIntent

    data class Link(
        val provider: String,
        val accessToken: String,
    ) : ConnectedAccountsIntent

    data class NotifyLinkError(
        val message: String,
    ) : ConnectedAccountsIntent

    data class ConsumeEvent(
        val event: ConnectedAccountsEvent,
    ) : ConnectedAccountsIntent
}

internal sealed interface ConnectedAccountsReducerEvent : ReducerEvent {
    data class Loaded(
        val accounts: List<SocialAccountState>,
    ) : ConnectedAccountsReducerEvent

    data class Failed(
        val message: String,
    ) : ConnectedAccountsReducerEvent

    data class AccountsChanged(
        val accounts: List<SocialAccountState>,
    ) : ConnectedAccountsReducerEvent

    data class Signal(
        val event: ConnectedAccountsEvent,
    ) : ConnectedAccountsReducerEvent

    data class EventConsumed(
        val event: ConnectedAccountsEvent,
    ) : ConnectedAccountsReducerEvent
}
