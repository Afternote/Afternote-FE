package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ConnectedAccountsViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : MviViewModel<ConnectedAccountsIntent, ConnectedAccountsUiState, ConnectedAccountsReducerEvent>(
            ConnectedAccountsUiState(isLoading = true),
        ) {
        override fun onIntent(intent: ConnectedAccountsIntent) {
            when (intent) {
                is ConnectedAccountsIntent.Toggle -> onToggle(intent.provider, intent.enabled)
                is ConnectedAccountsIntent.Link -> link(intent.provider, intent.accessToken)
                is ConnectedAccountsIntent.NotifyLinkError -> notifyLinkError(intent.message)
                is ConnectedAccountsIntent.ConsumeEvent -> dispatch(ConnectedAccountsReducerEvent.EventConsumed(intent.event))
            }
        }

        override fun reduce(
            state: ConnectedAccountsUiState,
            event: ConnectedAccountsReducerEvent,
        ): ConnectedAccountsUiState =
            when (event) {
                is ConnectedAccountsReducerEvent.Loaded -> {
                    state.copy(isLoading = false, accounts = event.accounts)
                }

                is ConnectedAccountsReducerEvent.Failed -> {
                    state.copy(isLoading = false, errorMessage = event.message)
                }

                is ConnectedAccountsReducerEvent.AccountsChanged -> {
                    state.copy(accounts = event.accounts)
                }

                is ConnectedAccountsReducerEvent.Signal -> {
                    state.copy(pendingEvent = event.event)
                }

                is ConnectedAccountsReducerEvent.EventConsumed -> {
                    if (state.pendingEvent == event.event) state.copy(pendingEvent = null) else state
                }
            }

        init {
            loadConnectedAccounts()
        }

        private fun loadConnectedAccounts() {
            viewModelScope.launch {
                runCatching { userRepository.getConnectedAccounts() }
                    .onSuccess { accounts ->
                        dispatch(ConnectedAccountsReducerEvent.Loaded(accounts.toStateList()))
                    }.onFailure {
                        dispatch(ConnectedAccountsReducerEvent.Failed("계정 정보를 불러올 수 없습니다."))
                    }
            }
        }

        private fun onToggle(
            provider: String,
            enabled: Boolean,
        ) {
            if (enabled) {
                dispatch(ConnectedAccountsReducerEvent.Signal(ConnectedAccountsEvent.RequestLink(provider)))
            } else {
                unlink(provider)
            }
        }

        private fun notifyLinkError(message: String) {
            dispatch(ConnectedAccountsReducerEvent.Signal(ConnectedAccountsEvent.ShowError(message)))
        }

        private fun link(
            provider: String,
            accessToken: String,
        ) {
            viewModelScope.launch {
                runCatching { userRepository.linkConnectedAccount(provider, accessToken) }
                    .onSuccess { accounts -> dispatch(ConnectedAccountsReducerEvent.AccountsChanged(accounts.toStateList())) }
                    .onFailure { dispatch(ConnectedAccountsReducerEvent.Failed("계정 연결에 실패했습니다.")) }
            }
        }

        private fun unlink(provider: String) {
            viewModelScope.launch {
                runCatching { userRepository.unlinkConnectedAccount(provider) }
                    .onSuccess { accounts -> dispatch(ConnectedAccountsReducerEvent.AccountsChanged(accounts.toStateList())) }
                    .onFailure { dispatch(ConnectedAccountsReducerEvent.Failed("계정 연결 해제에 실패했습니다.")) }
            }
        }

        private fun UserConnectedAccount.toStateList(): List<SocialAccountState> =
            listOf(
                SocialAccountState(
                    provider = "naver",
                    iconRes = R.drawable.ic_naver_logo,
                    labelRes = R.string.login_with_naver,
                    isConnected = naver,
                    isLinkable = false,
                    email = naverEmail,
                ),
                SocialAccountState(
                    provider = "google",
                    iconRes = R.drawable.ic_google_logo,
                    labelRes = R.string.login_with_google,
                    isConnected = google,
                    isLinkable = true,
                    email = googleEmail,
                ),
                SocialAccountState(
                    provider = "kakao",
                    iconRes = R.drawable.ic_kakao_logo,
                    labelRes = R.string.login_with_kakao,
                    isConnected = kakao,
                    isLinkable = true,
                    email = kakaoEmail,
                ),
                SocialAccountState(
                    provider = "apple",
                    iconRes = R.drawable.ic_apple_logo,
                    labelRes = R.string.login_with_apple,
                    isConnected = apple,
                    isLinkable = false,
                    email = appleEmail,
                ),
            )
    }
