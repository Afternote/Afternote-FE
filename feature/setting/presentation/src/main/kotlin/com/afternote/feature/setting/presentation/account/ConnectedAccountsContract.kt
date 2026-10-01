package com.afternote.feature.setting.presentation.account

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ConnectedAccountsIntent : MviIntent {
    data class Toggle(
        val provider: String,
        val enabled: Boolean,
    ) : ConnectedAccountsIntent

    /** 화면이 플랫폼 인증으로 받아낸 [accessToken] 으로 [provider] 를 연결한다. */
    data class Link(
        val provider: String,
        val accessToken: String,
    ) : ConnectedAccountsIntent

    /** 화면이 처리한 연결 요청 [provider] 를 되돌려준다. 그사이 다른 제공자 요청이 올라왔으면 그 요청은 남는다. */
    data class ConsumeLinkRequest(
        val provider: String,
    ) : ConnectedAccountsIntent
}

internal sealed interface ConnectedAccountsReducerEvent : ReducerEvent {
    data class Loaded(
        val accounts: List<SocialAccountState>,
    ) : ConnectedAccountsReducerEvent

    data class LoadFailed(
        val message: String,
    ) : ConnectedAccountsReducerEvent

    data class AccountsChanged(
        val accounts: List<SocialAccountState>,
    ) : ConnectedAccountsReducerEvent

    data class ChangeFailed(
        val message: String,
    ) : ConnectedAccountsReducerEvent

    data class LinkRequested(
        val provider: String,
    ) : ConnectedAccountsReducerEvent

    data class LinkRequestConsumed(
        val provider: String,
    ) : ConnectedAccountsReducerEvent
}
