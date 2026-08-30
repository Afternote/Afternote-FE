package com.afternote.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.domain.usecase.auth.PasskeyLoginUseCase
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.reporting.AuthFailureStage
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import com.afternote.feature.onboarding.presentation.reporting.recordAuthFailure
import com.afternote.feature.onboarding.presentation.toDisplayMessage
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
        private val passkeyLoginUseCase: PasskeyLoginUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState = _uiState.asStateFlow()

        /** 네트워크 실패 팝업의 "다시 시도하기" 가 재실행할 마지막 시도. */
        private var lastAttempt: LoginType? = null

        /**
         * 이 화면 인스턴스에서 패스키 자동 시도를 이미 걸었는지.
         *
         * 구성 변경으로 컴포지션이 다시 만들어져도 시스템 선택기를 두 번 띄우지 않기 위한 것이다 —
         * ViewModel 은 구성 변경을 넘겨 살아남으므로 이 플래그가 그 경계 역할을 한다.
         */
        private var passkeyAttempted = false

        fun updateEmail(value: String) {
            // 입력이 바뀌면 앞선 자격 거절은 더 이상 이 입력의 판정이 아니다.
            _uiState.update { it.copy(email = value, hasCredentialError = false) }
        }

        fun updatePassword(value: String) {
            _uiState.update { it.copy(password = value, hasCredentialError = false) }
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
         * 화면 진입 시 1회, 패스키 인증 옵션을 미리 받아 둔다.
         *
         * 로그인 수단별 전용 버튼을 두지 않는 것이 [공식 UX 권고](https://developer.android.com/design/ui/mobile/guides/patterns/passkeys)
         * 라, 진입점은 버튼이 아니라 화면 진입 자체다. 성공하면 [LoginUiState.passkeyRequestJson]
         * 으로 UI 에 넘겨 시스템 선택기를 띄우게 한다.
         *
         * **실패는 화면에 알리지 않는다.** 사용자가 요청한 적 없는 시도라, 여기서 오류를 띄우면
         * 이메일 로그인을 하러 온 사람에게 설명할 수 없는 안내가 뜬다 — 기존 로그인 폼은 무간섭으로 남는다.
         */
        fun startPasskeyLogin() {
            if (passkeyAttempted) return
            passkeyAttempted = true
            viewModelScope.launch {
                passkeyLoginUseCase
                    .requestOptions()
                    .onSuccess { options ->
                        _uiState.update { it.copy(passkeyRequestJson = options.requestJson) }
                    }.onFailure { exception ->
                        if (exception is CancellationException) throw exception
                        // 오프라인은 장애가 아니다 — 비행기 모드로 앱을 연 것만으로 기록이 쌓이면
                        // 보관 한도를 정상 상황이 차지한다. 그 밖의 실패는 이 단계가 통째로 죽어도
                        // 화면에 흔적이 없으므로 계측이 유일한 신호다.
                        if (exception !is CoreAuthFailure.NetworkUnavailable) {
                            errorReporter.recordAuthFailure(
                                stage = AuthFailureStage.PASSKEY_OPTIONS,
                                throwable = exception,
                                provider = AuthProvider.PASSKEY,
                            )
                        }
                    }
            }
        }

        /** UI 가 [LoginUiState.passkeyRequestJson] 을 집어 Credential Manager 로 넘긴 뒤 reset. */
        fun onPasskeyRequestConsumed() {
            _uiState.update { it.copy(passkeyRequestJson = null) }
        }

        /**
         * Credential Manager 단계의 실패를 기록한다. 서버 호출 이전이라 [login] 경로를 타지 않는다.
         *
         * 정상 이탈(이 기기에 패스키 없음 · 사용자가 시트를 닫음)은 UI 가 걸러서 넘기지 않는다.
         */
        fun onPasskeyAssertionFailed(throwable: Throwable) {
            errorReporter.recordAuthFailure(
                stage = AuthFailureStage.PASSKEY_ASSERTION,
                throwable = throwable,
                provider = AuthProvider.PASSKEY,
            )
        }

        /**
         * 받아온 assertion 으로 서버 검증·로그인을 마친다.
         *
         * 실패 표시가 [login] 과 다르다 — 인라인도 재시도 팝업도 쓰지 않고 스낵바 하나로 모은다.
         * 인라인은 걸 입력 필드가 없고, 재시도 팝업의 "다시 시도하기" 는 [lastAttempt]
         * ([LoginType]) 를 재실행하는 장치라 패스키에는 재실행할 대상이 없다 — 띄우면 눌러도
         * 아무 일도 일어나지 않는 버튼이 된다.
         */
        fun loginWithPasskey(assertionJson: String) {
            if (_uiState.value.isLoading) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, hasCredentialError = false) }
                passkeyLoginUseCase(assertionJson)
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    }.onFailure { exception ->
                        if (exception is CancellationException) throw exception
                        errorReporter.recordAuthFailure(
                            stage = AuthFailureStage.LOGIN,
                            throwable = exception,
                            provider = AuthProvider.PASSKEY,
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = exception.toDisplayMessage(R.string.onboarding_login_passkey_failed),
                            )
                        }
                    }
            }
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

        /** 네트워크 실패 팝업의 "다시 시도하기" — 마지막 시도를 같은 자격으로 재실행한다. */
        fun retryLogin() {
            _uiState.update { it.copy(showNetworkErrorPopup = false) }
            lastAttempt?.let(::login)
        }

        fun onNetworkErrorDismissed() {
            _uiState.update { it.copy(showNetworkErrorPopup = false) }
        }

        private fun login(loginType: LoginType) {
            if (_uiState.value.isLoading) return
            lastAttempt = loginType
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, hasCredentialError = false) }
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
                        // 자격 거절은 계측하지 않는다 — 비밀번호 오타는 사용자의 정상적인 입력 실수다
                        // (아이디 찾기 인증번호 오타와 같은 판단, FindIdViewModel.verifyCode).
                        if (exception !is CoreAuthFailure.InvalidLoginCredentials) {
                            errorReporter.recordAuthFailure(
                                stage = AuthFailureStage.LOGIN,
                                throwable = exception,
                                provider = loginType.authProvider,
                            )
                        }
                        _uiState.update {
                            // 루트로 좁혀 `when` 을 exhaustive 하게 만든다 — 사유가 늘면 여기가 컴파일 에러로
                            // 잡힌다. `else` 로 뭉개 두면 인라인·팝업으로 갈라야 할 새 사유가 조용히 문구 하나로
                            // 흘러간다. 사유를 확인하지 못한 실패(null)는 계속 문구 매핑에 맡긴다.
                            when (exception as? CoreAuthFailure) {
                                is CoreAuthFailure.InvalidLoginCredentials -> {
                                    it.copy(isLoading = false, hasCredentialError = true)
                                }

                                is CoreAuthFailure.NetworkUnavailable -> {
                                    it.copy(isLoading = false, showNetworkErrorPopup = true)
                                }

                                is CoreAuthFailure.EmailAlreadyRegistered,
                                is CoreAuthFailure.EmailVerification,
                                is CoreAuthFailure.SocialLoginRejected,
                                is CoreAuthFailure.UserCancelledAuth,
                                null,
                                -> {
                                    it.copy(isLoading = false, errorMessage = exception.toDisplayMessage(R.string.onboarding_login_failed))
                                }
                            }
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
