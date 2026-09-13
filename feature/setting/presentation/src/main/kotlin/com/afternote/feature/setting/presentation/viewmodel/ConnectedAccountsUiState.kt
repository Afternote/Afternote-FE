package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.UiState

internal data class ConnectedAccountsUiState(
    val isLoading: Boolean = false,
    val accounts: List<SocialAccountState> = emptyList(),
    val errorMessage: String? = null,
    val pendingEvent: ConnectedAccountsEvent? = null,
) : UiState

internal sealed interface ConnectedAccountsEvent {
    data class RequestLink(
        val provider: String,
    ) : ConnectedAccountsEvent

    data class ShowError(
        val message: String,
    ) : ConnectedAccountsEvent
}
