package com.afternote.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.LoginRejectedException
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.R
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
                        // 리포지토리가 이미 되던지지만(mapLoginFailure), UseCase 등 다른 suspend 경계를
                        // 감싼 runCatching 이 취소를 Result.failure 로 만들 수 있어 여기서도 막는다(전수 정정은 #661).
                        if (exception is CancellationException) throw exception
                        errorReporter.recordAuthFailure(
                            stage = AuthFailureStage.LOGIN,
                            throwable = exception,
                            provider = loginType.authProvider,
                        )
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

private val LoginType.authProvider: AuthProvider
    get() =
        when (this) {
            is LoginType.Email -> AuthProvider.EMAIL
            is LoginType.Kakao -> AuthProvider.KAKAO
            is LoginType.Google -> AuthProvider.GOOGLE
        }
