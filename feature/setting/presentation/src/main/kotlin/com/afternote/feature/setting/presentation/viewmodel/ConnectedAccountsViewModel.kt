package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
        private var loadJob: Job? = null
        private var mutationJob: Job? = null

        override fun onIntent(intent: ConnectedAccountsIntent) {
            when (intent) {
                ConnectedAccountsIntent.RetryLoad -> loadConnectedAccounts()
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
                ConnectedAccountsReducerEvent.Loading -> {
                    state.copy(isLoading = true, errorMessage = null)
                }

                is ConnectedAccountsReducerEvent.Loaded -> {
                    state.copy(isLoading = false, accounts = event.accounts, errorMessage = null)
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
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    dispatch(ConnectedAccountsReducerEvent.Loading)
                    runCatchingCancellable { userRepository.getConnectedAccounts() }
                        .onSuccess { accounts ->
                            dispatch(ConnectedAccountsReducerEvent.Loaded(accounts.toStateList()))
                        }.onFailure {
                            dispatch(ConnectedAccountsReducerEvent.Failed(UiText.Resource(R.string.setting_connected_accounts_load_error)))
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
            if (mutationJob?.isActive == true) return
            mutationJob =
                viewModelScope.launch {
                    runCatchingCancellable { userRepository.linkConnectedAccount(provider, accessToken) }
                        .onSuccess { accounts -> dispatch(ConnectedAccountsReducerEvent.AccountsChanged(accounts.toStateList())) }
                        .onFailure { dispatch(ConnectedAccountsReducerEvent.Signal(ConnectedAccountsEvent.ShowError("계정 연결에 실패했습니다."))) }
                }
        }

        private fun unlink(provider: String) {
            if (mutationJob?.isActive == true) return
            mutationJob =
                viewModelScope.launch {
                    runCatchingCancellable { userRepository.unlinkConnectedAccount(provider) }
                        .onSuccess { accounts -> dispatch(ConnectedAccountsReducerEvent.AccountsChanged(accounts.toStateList())) }
                        .onFailure { dispatch(ConnectedAccountsReducerEvent.Signal(ConnectedAccountsEvent.ShowError("계정 연결 해제에 실패했습니다."))) }
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
