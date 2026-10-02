package com.afternote.feature.setting.presentation.account

import com.afternote.core.ui.mvi.UiState

data class ConnectedAccountsUiState(
    val isLoading: Boolean = false,
    val accounts: List<SocialAccountState> = emptyList(),
    val errorMessage: String? = null,
    val isUpdating: Boolean = false,
    val pendingError: String? = null,
    /**
     * 화면이 플랫폼 인증(카카오 SDK·Credential Manager)을 시작해야 하는 제공자 신호다 (#1502).
     *
     * ViewModel 은 Activity·Context 에 닿지 않으므로 토큰을 직접 받지 못한다. 화면이 이 신호를 받아
     * 인증을 돌린 뒤 토큰만 [ConnectedAccountsIntent.Link] 로 돌려보내고, 받은 즉시
     * [ConnectedAccountsIntent.ConsumeLinkRequest] 로 되돌린다.
     */
    val pendingLinkProvider: String? = null,
) : UiState
