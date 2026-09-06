package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.UserConnectedAccount
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

        /** 진행 중인 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null
        private var accountUpdateJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — VM 필드인 이유는 ReceiverHomeViewModel 의 refreshOnReturn 과
         * 동일, 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            loadConnectedAccounts()
        }

        /** 다른 화면에서 복귀했을 때의 자동 갱신 (#701). 첫 진입은 건너뛰고, 로드가 겹치면 건너뛴다. */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true || accountUpdateJob?.isActive == true) return
            loadConnectedAccounts(keepsStateOnFailure = true)
        }

        private fun loadConnectedAccounts(keepsStateOnFailure: Boolean = false) {
            loadJob =
                viewModelScope.launch {
                    runCatchingCancellable { userRepository.getConnectedAccounts() }
                        .onSuccess { accounts ->
                            _uiState.update { it.copy(isLoading = false, accounts = accounts.toStateList(), errorMessage = null) }
                        }.onFailure {
                            if (!keepsStateOnFailure || _uiState.value.accounts.isEmpty()) {
                                _uiState.update { it.copy(isLoading = false, errorMessage = "계정 정보를 불러올 수 없습니다.") }
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
            updateConnectedAccounts(errorMessage = "계정 연결에 실패했습니다.") {
                userRepository.linkConnectedAccount(provider, accessToken)
            }
        }

        private fun unlink(provider: String) {
            updateConnectedAccounts(errorMessage = "계정 연결 해제에 실패했습니다.") {
                userRepository.unlinkConnectedAccount(provider)
            }
        }

        private fun updateConnectedAccounts(
            errorMessage: String,
            update: suspend () -> UserConnectedAccount,
        ) {
            if (accountUpdateJob?.isActive == true) return
            loadJob?.cancel()
            accountUpdateJob =
                viewModelScope.launch {
                    runCatchingCancellable { update() }
                        .onSuccess { accounts ->
                            _uiState.update { it.copy(isLoading = false, accounts = accounts.toStateList(), errorMessage = null) }
                        }.onFailure {
                            _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                        }
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
