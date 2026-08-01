package com.afternote.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.feature.onboarding.presentation.reporting.AuthFailureStage
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import com.afternote.feature.onboarding.presentation.reporting.recordAuthFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val loginUseCase: LoginUseCase,
        private val errorReporter: ErrorReporter,
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

        /**
         * 소셜 SDK 에서 토큰을 받아오지 못한 실패를 기록한다.
         *
         * 이 단계는 서버 호출 이전이라 [login] 경로를 타지 않아, UI 가 직접 알려주지 않으면
         * 콘솔에 아무 흔적도 남지 않는다. 사용자 취소는 오류가 아니므로 UI 가 걸러서 호출한다.
         */
        fun onSocialTokenRequestFailed(
            provider: AuthProvider,
            throwable: Throwable,
        ) {
            errorReporter.recordAuthFailure(
                stage = AuthFailureStage.SOCIAL_TOKEN_REQUEST,
                throwable = throwable,
                provider = provider,
            )
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
                        // 화면 이탈로 스코프가 취소된 것을 장애로 기록하거나 실패 UI 로 소비하지 않는다.
                        // suspend 호출을 감싼 runCatching 이 취소까지 Result.failure 로 만들기 때문에
                        // 여기까지 들어온다 — 전파를 끊지 않도록 즉시 되던진다(전수 정정은 #661).
                        if (exception is CancellationException) throw exception
                        errorReporter.recordAuthFailure(
                            stage = AuthFailureStage.LOGIN,
                            throwable = exception,
                            provider = loginType.authProvider,
                        )
                        _uiState.update {
                            // message 가 null 이면 빈 문자열로 두어 실패 사실이 소실되지 않게 한다.
                            // (null 은 "에러 없음"과 구분되지 않아 스낵바가 무음 처리되던 버그. UI 가 ifBlank 로 일반 문구 폴백.)
                            it.copy(isLoading = false, errorMessage = exception.message.orEmpty())
                        }
                    }
            }
        }
    }

private val LoginType.authProvider: AuthProvider
    get() =
        when (this) {
            is LoginType.Email -> AuthProvider.EMAIL
            is LoginType.Kakao -> AuthProvider.KAKAO
            is LoginType.Google -> AuthProvider.GOOGLE
        }
