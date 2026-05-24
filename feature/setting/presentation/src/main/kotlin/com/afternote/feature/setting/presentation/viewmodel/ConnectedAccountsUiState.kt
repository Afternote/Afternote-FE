package com.afternote.feature.setting.presentation.viewmodel

data class ConnectedAccountsUiState(
    val isLoading: Boolean = false,
    val accounts: List<SocialAccountState> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface ConnectedAccountsEvent {
    data class RequestLink(
        val provider: String,
    ) : ConnectedAccountsEvent

    data class ShowError(
        val message: String,
    ) : ConnectedAccountsEvent
}
