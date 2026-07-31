package com.afternote.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                        // 전송 계층 실패는 예외 원문(영문 기술 메시지)을 숨기고 네트워크 안내로 고정한다.
                        // 그 외에는 예외 message(서버 응답 실패면 인터셉터가 채운 문구)를 쓰되,
                        // null/blank 면 일반 문구로 폴백해 실패 사실이 소실되지 않게 한다 (#502 무음 버그 계약 유지).
                        val errorMessage =
                            when (exception) {
                                is NetworkUnavailableException -> {
                                    UiText.Resource(R.string.login_network_error)
                                }

                                else -> {
                                    UiText.DynamicOrResource(
                                        value = exception.message?.takeUnless { it.isBlank() },
                                        fallbackResId = R.string.login_failed,
                                    )
                                }
                            }
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = errorMessage)
                        }
                    }
            }
        }
    }
