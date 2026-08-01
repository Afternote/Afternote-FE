package com.afternote.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.error.LoginRejectedException
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val loginUseCase: LoginUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState = _uiState.asStateFlow()

        fun updateEmail(value: String) {
            _uiState.update { it.copy(email = value) }
        }

        fun updatePassword(value: String) {
            _uiState.update { it.copy(password = value) }
        }

        fun loginWithEmail() {
            val state = _uiState.value
            login(
                LoginType.Email(
                    email = state.email,
                    password = state.password,
                ),
            )
        }

        fun loginWithKakao(oauthToken: String) {
            login(LoginType.Kakao(oauthToken))
        }

        fun loginWithGoogle(idToken: String) {
            login(LoginType.Google(idToken))
        }

        /** UI 가 [LoginUiState.isLoggedIn] 소비 후 reset. */
        fun onLoggedInConsumed() {
            _uiState.update { it.copy(isLoggedIn = false) }
        }

        /** UI 가 [LoginUiState.shouldStartOnboarding] 소비 후 reset. */
        fun onOnboardingStartConsumed() {
            _uiState.update { it.copy(shouldStartOnboarding = false) }
        }

        /** UI 가 [LoginUiState.errorMessage] 소비 (snackbar 표시) 후 reset. */
        fun onErrorConsumed() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun login(loginType: LoginType) {
            if (_uiState.value.isLoading) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val result = loginUseCase(loginType = loginType)
                result
                    .onSuccess { isNewUser ->
                        _uiState.update {
                            if (isNewUser) {
                                it.copy(isLoading = false, shouldStartOnboarding = true)
                            } else {
                                it.copy(isLoading = false, isLoggedIn = true)
                            }
                        }
                    }.onFailure { exception ->
                        // 예외 message 를 표시값으로 쓰지 않는다 — 서버 5xx 본문(내부 SQL 실측 #511)·
                        // 역직렬화 원문이 그 경로로 샌다. 사유가 확인된 두 타입만 문구를 갖는다.
                        val errorMessage =
                            when (exception) {
                                is NetworkUnavailableException -> {
                                    UiText.Resource(R.string.login_network_error)
                                }

                                is LoginRejectedException -> {
                                    UiText.Dynamic(exception.displayMessage)
                                }

                                else -> {
                                    UiText.Resource(R.string.login_failed)
                                }
                            }
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = errorMessage)
                        }
                    }
            }
        }
    }
