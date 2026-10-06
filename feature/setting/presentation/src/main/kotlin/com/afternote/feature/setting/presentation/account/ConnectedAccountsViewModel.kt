package com.afternote.feature.setting.presentation.account

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.SettingAccountRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ConnectedAccountsViewModel
    @Inject
    constructor(
        private val accountRepository: SettingAccountRepository,
    ) : MviViewModel<ConnectedAccountsIntent, ConnectedAccountsUiState, ConnectedAccountsReducerEvent>(
            ConnectedAccountsUiState(isLoading = true),
        ) {
        private var loadJob: Job? = null

        init {
            loadConnectedAccounts()
        }

        override fun onIntent(intent: ConnectedAccountsIntent) {
            when (intent) {
                ConnectedAccountsIntent.RetryLoad -> {
                    if (currentState.errorMessage != null) loadConnectedAccounts()
                }

                is ConnectedAccountsIntent.ConsumeError -> {
                    dispatch(ConnectedAccountsReducerEvent.ErrorConsumed(intent.message))
                }

                is ConnectedAccountsIntent.Toggle -> {
                    toggle(intent.provider, intent.enabled)
                }

                is ConnectedAccountsIntent.Link -> {
                    link(intent.provider, intent.accessToken)
                }

                is ConnectedAccountsIntent.ConsumeLinkRequest -> {
                    dispatch(
                        ConnectedAccountsReducerEvent.LinkRequestConsumed(intent.provider),
                    )
                }
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

                ConnectedAccountsReducerEvent.Changing -> {
                    state.copy(isUpdating = true, pendingError = null)
                }

                is ConnectedAccountsReducerEvent.ErrorConsumed -> {
                    if (state.pendingError ==
                        event.message
                    ) {
                        state.copy(pendingError = null)
                    } else {
                        state
                    }
                }

                is ConnectedAccountsReducerEvent.Loaded -> {
                    state.copy(isLoading = false, errorMessage = null, accounts = event.accounts)
                }

                is ConnectedAccountsReducerEvent.LoadFailed -> {
                    state.copy(isLoading = false, errorMessage = event.message)
                }

                is ConnectedAccountsReducerEvent.AccountsChanged -> {
                    state.copy(accounts = event.accounts, isUpdating = false)
                }

                is ConnectedAccountsReducerEvent.ChangeFailed -> {
                    state.copy(pendingError = event.message, isUpdating = false)
                }

                is ConnectedAccountsReducerEvent.LinkRequested -> {
                    state.copy(pendingLinkProvider = event.provider)
                }

                // 늦게 도착한 소비가 그사이 올라온 다른 제공자 요청을 지우지 않게 한다.
                is ConnectedAccountsReducerEvent.LinkRequestConsumed -> {
                    if (state.pendingLinkProvider == event.provider) state.copy(pendingLinkProvider = null) else state
                }
            }

        private fun loadConnectedAccounts() {
            if (loadJob?.isActive == true) return
            dispatch(ConnectedAccountsReducerEvent.Loading)
            loadJob =
                viewModelScope.launch {
                    runCatchingCancellable { accountRepository.getConnectedAccounts() }
                        .onSuccess { accounts ->
                            dispatch(ConnectedAccountsReducerEvent.Loaded(accounts.toStateList()))
                        }.onFailure {
                            dispatch(ConnectedAccountsReducerEvent.LoadFailed("계정 정보를 불러올 수 없습니다."))
                        }
                }
        }

        private fun toggle(
            provider: String,
            enabled: Boolean,
        ) {
            if (currentState.isLoading || currentState.errorMessage != null || currentState.isUpdating) return
            if (enabled) {
                dispatch(ConnectedAccountsReducerEvent.LinkRequested(provider))
            } else {
                unlink(provider)
            }
        }

        private fun link(
            provider: String,
            accessToken: String,
        ) {
            if (currentState.isLoading || currentState.errorMessage != null || currentState.isUpdating) return
            dispatch(ConnectedAccountsReducerEvent.Changing)
            viewModelScope.launch {
                runCatchingCancellable { accountRepository.linkConnectedAccount(provider, accessToken) }
                    .onSuccess { accounts -> dispatch(ConnectedAccountsReducerEvent.AccountsChanged(accounts.toStateList())) }
                    .onFailure { dispatch(ConnectedAccountsReducerEvent.ChangeFailed("계정 연결에 실패했습니다.")) }
            }
        }

        private fun unlink(provider: String) {
            dispatch(ConnectedAccountsReducerEvent.Changing)
            viewModelScope.launch {
                runCatchingCancellable { accountRepository.unlinkConnectedAccount(provider) }
                    .onSuccess { accounts -> dispatch(ConnectedAccountsReducerEvent.AccountsChanged(accounts.toStateList())) }
                    .onFailure { dispatch(ConnectedAccountsReducerEvent.ChangeFailed("계정 연결 해제에 실패했습니다.")) }
            }
        }

        private fun UserConnectedAccount.toStateList(): List<SocialAccountState> =
            listOf(
                SocialAccountState(
                    provider = "naver",
                    iconRes = R.drawable.setting_ic_naver_logo,
                    labelRes = R.string.setting_login_with_naver,
                    isConnected = naver,
                    isLinkable = false,
                    email = naverEmail,
                ),
                SocialAccountState(
                    provider = "google",
                    iconRes = R.drawable.setting_ic_google_logo,
                    labelRes = R.string.setting_login_with_google,
                    isConnected = google,
                    isLinkable = true,
                    email = googleEmail,
                ),
                SocialAccountState(
                    provider = "kakao",
                    iconRes = R.drawable.setting_ic_kakao_logo,
                    labelRes = R.string.setting_login_with_kakao,
                    isConnected = kakao,
                    isLinkable = true,
                    email = kakaoEmail,
                ),
                SocialAccountState(
                    provider = "apple",
                    iconRes = R.drawable.setting_ic_apple_logo,
                    labelRes = R.string.setting_login_with_apple,
                    isConnected = apple,
                    isLinkable = false,
                    email = appleEmail,
                ),
            )
    }
