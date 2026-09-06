package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectedAccountsViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ConnectedAccountsUiState(isLoading = true))
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<ConnectedAccountsEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()
        private var loadJob: Job? = null
        private var mutationJob: Job? = null

        init {
            loadConnectedAccounts()
        }

        fun retryLoadConnectedAccounts() {
            loadConnectedAccounts()
        }

        private fun loadConnectedAccounts() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    runCatchingCancellable { userRepository.getConnectedAccounts() }
                        .onSuccess { accounts ->
                            _uiState.update {
                                it.copy(isLoading = false, accounts = accounts.toStateList(), errorMessage = null)
                            }
                        }.onFailure {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.Resource(R.string.setting_connected_accounts_load_error),
                                )
                            }
                        }
                }
        }

        fun onToggle(
            provider: String,
            enabled: Boolean,
        ) {
            if (enabled) {
                viewModelScope.launch { _events.send(ConnectedAccountsEvent.RequestLink(provider)) }
            } else {
                unlink(provider)
            }
        }

        fun notifyLinkError(message: String) {
            viewModelScope.launch { _events.send(ConnectedAccountsEvent.ShowError(message)) }
        }

        fun link(
            provider: String,
            accessToken: String,
        ) {
            if (mutationJob?.isActive == true) return
            mutationJob =
                viewModelScope.launch {
                    runCatchingCancellable { userRepository.linkConnectedAccount(provider, accessToken) }
                        .onSuccess { accounts -> _uiState.update { it.copy(accounts = accounts.toStateList()) } }
                        .onFailure { _events.send(ConnectedAccountsEvent.ShowError("계정 연결에 실패했습니다.")) }
                }
        }

        private fun unlink(provider: String) {
            if (mutationJob?.isActive == true) return
            mutationJob =
                viewModelScope.launch {
                    runCatchingCancellable { userRepository.unlinkConnectedAccount(provider) }
                        .onSuccess { accounts -> _uiState.update { it.copy(accounts = accounts.toStateList()) } }
                        .onFailure { _events.send(ConnectedAccountsEvent.ShowError("계정 연결 해제에 실패했습니다.")) }
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
