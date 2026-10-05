package com.afternote.feature.setting.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.feature.setting.domain.SettingAccountRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ConnectedAccountsViewModel
    @Inject
    constructor(
        private val accountRepository: SettingAccountRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ConnectedAccountsUiState(isLoading = true))
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<ConnectedAccountsEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            loadConnectedAccounts()
        }

        private fun loadConnectedAccounts() {
            viewModelScope.launch {
                runCatchingCancellable { accountRepository.getConnectedAccounts() }
                    .onSuccess { accounts ->
                        _uiState.update { it.copy(isLoading = false, accounts = accounts.toStateList()) }
                    }.onFailure {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "계정 정보를 불러올 수 없습니다.") }
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
            viewModelScope.launch {
                runCatchingCancellable { accountRepository.linkConnectedAccount(provider, accessToken) }
                    .onSuccess { accounts -> _uiState.update { it.copy(accounts = accounts.toStateList()) } }
                    .onFailure { _uiState.update { it.copy(errorMessage = "계정 연결에 실패했습니다.") } }
            }
        }

        private fun unlink(provider: String) {
            viewModelScope.launch {
                runCatchingCancellable { accountRepository.unlinkConnectedAccount(provider) }
                    .onSuccess { accounts -> _uiState.update { it.copy(accounts = accounts.toStateList()) } }
                    .onFailure { _uiState.update { it.copy(errorMessage = "계정 연결 해제에 실패했습니다.") } }
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
