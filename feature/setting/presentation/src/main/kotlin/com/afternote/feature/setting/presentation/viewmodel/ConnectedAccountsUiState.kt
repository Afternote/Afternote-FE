package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.UiText

data class ConnectedAccountsUiState(
    val isLoading: Boolean = false,
    val accounts: List<SocialAccountState> = emptyList(),
    val errorMessage: UiText? = null,
)

sealed interface ConnectedAccountsEvent {
    data class RequestLink(
        val provider: String,
    ) : ConnectedAccountsEvent

    data class ShowError(
        val message: String,
    ) : ConnectedAccountsEvent
}
