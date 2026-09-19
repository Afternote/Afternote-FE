package com.afternote.feature.onboarding.presentation.login

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.onboarding.presentation.OnboardingFailure
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.reporting.AuthFailureStage
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import com.afternote.feature.onboarding.presentation.reporting.recordAuthFailure
import com.afternote.feature.onboarding.presentation.toDisplayMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * 로그인 ViewModel.
 *
 * 전이는 [reduce] 한 곳이고 진입점은 [onIntent] 하나다 (#1802). 기준은 `docs/convention/mvi.md`.
 */
@HiltViewModel
internal class LoginViewModel
    @Inject
    constructor(
        private val loginUseCase: LoginUseCase,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<LoginIntent, LoginUiState, LoginReducerEvent>(LoginUiState()) {
        /** 네트워크 실패 팝업의 "다시 시도하기" 가 재실행할 마지막 시도. */
        private var lastAttempt: LoginType? = null

        override fun onIntent(intent: LoginIntent) {
            when (intent) {
                is LoginIntent.UpdateEmail -> {
                    dispatch(LoginReducerEvent.EmailChanged(intent.value))
                }

                is LoginIntent.UpdatePassword -> {
                    dispatch(LoginReducerEvent.PasswordChanged(intent.value))
                }

                LoginIntent.SubmitEmailLogin -> {
                    login(
                        LoginType.Email(
                            email = currentState.email,
                            password = currentState.password,
                        ),
                    )
                }

                is LoginIntent.SubmitKakaoLogin -> {
                    login(LoginType.Kakao(intent.oauthToken))
                }

                is LoginIntent.SubmitGoogleLogin -> {
                    login(LoginType.Google(intent.idToken))
                }

                is LoginIntent.ReportSocialTokenFailure -> {
                    errorReporter.recordAuthFailure(
                        stage = AuthFailureStage.SOCIAL_TOKEN_REQUEST,
                        throwable = intent.throwable,
                        provider = intent.provider,
                    )
                }

                LoginIntent.RetryLogin -> {
                    retryLogin()
                }

                LoginIntent.DismissNetworkError -> {
                    dispatch(LoginReducerEvent.NetworkErrorDismissed)
                }

                LoginIntent.ConsumeLoggedIn -> {
                    dispatch(LoginReducerEvent.LoggedInConsumed)
                }

                LoginIntent.ConsumeOnboardingStart -> {
                    dispatch(LoginReducerEvent.OnboardingStartConsumed)
                }

                LoginIntent.ConsumeError -> {
                    dispatch(LoginReducerEvent.ErrorConsumed)
                }
            }
        }

        override fun reduce(
            state: LoginUiState,
            event: LoginReducerEvent,
        ): LoginUiState =
            when (event) {
                // 입력이 바뀌면 앞선 자격 거절은 더 이상 이 입력의 판정이 아니다.
                is LoginReducerEvent.EmailChanged -> {
                    state.copy(
                        email = event.value,
                        failure =
                            state.failure.takeIf {
                                state.email ==
                                    event.value
                            },
                    )
                }

                is LoginReducerEvent.PasswordChanged -> {
                    state.copy(
                        password = event.value,
                        failure =
                            state.failure.takeIf {
                                state.password ==
                                    event.value
                            },
                    )
                }

                LoginReducerEvent.LoginStarted -> {
                    state.copy(isLoading = true, failure = null)
                }

                LoginReducerEvent.LoggedIn -> {
                    state.copy(isLoading = false, isLoggedIn = true)
                }

                LoginReducerEvent.OnboardingRequired -> {
                    state.copy(isLoading = false, shouldStartOnboarding = true)
                }

                LoginReducerEvent.CredentialRejected -> {
                    state.copy(isLoading = false, failure = OnboardingFailure.CredentialsRejected)
                }

                LoginReducerEvent.NetworkErrorRaised -> {
                    state.copy(isLoading = false, failure = OnboardingFailure.LoginNetworkUnavailable)
                }

                is LoginReducerEvent.LoginFailed -> {
                    state.copy(isLoading = false, failure = OnboardingFailure.RequestFailed(event.message))
                }

                LoginReducerEvent.NetworkErrorDismissed -> {
                    state.copy(
                        failure =
                            state.failure.takeUnless {
                                it ==
                                    OnboardingFailure.LoginNetworkUnavailable
                            },
                    )
                }

                LoginReducerEvent.LoggedInConsumed -> {
                    state.copy(isLoggedIn = false)
                }

                LoginReducerEvent.OnboardingStartConsumed -> {
                    state.copy(shouldStartOnboarding = false)
                }

                LoginReducerEvent.ErrorConsumed -> {
                    state.copy(failure = state.failure.takeUnless { it is OnboardingFailure.RequestFailed })
                }
            }

        /** 마지막 시도를 같은 자격으로 재실행한다. */
        private fun retryLogin() {
            dispatch(LoginReducerEvent.NetworkErrorDismissed)
            lastAttempt?.let(::login)
        }

        private fun login(loginType: LoginType) {
            if (currentState.isLoading) return
            lastAttempt = loginType
            viewModelScope.launch {
                dispatch(LoginReducerEvent.LoginStarted)
                loginUseCase(loginType = loginType)
                    .onSuccess { isNewUser ->
                        if (isNewUser) {
                            dispatch(LoginReducerEvent.OnboardingRequired)
                        } else {
                            dispatch(LoginReducerEvent.LoggedIn)
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
                        // 루트로 좁혀 `when` 을 exhaustive 하게 만든다 — 사유가 늘면 여기가 컴파일 에러로
                        // 잡힌다. `else` 로 뭉개 두면 인라인·팝업으로 갈라야 할 새 사유가 조용히 문구 하나로
                        // 흘러간다. 사유를 확인하지 못한 실패(null)는 계속 문구 매핑에 맡긴다.
                        val event =
                            when (exception as? CoreAuthFailure) {
                                is CoreAuthFailure.InvalidLoginCredentials -> {
                                    LoginReducerEvent.CredentialRejected
                                }

                                is CoreAuthFailure.NetworkUnavailable -> {
                                    LoginReducerEvent.NetworkErrorRaised
                                }

                                // 소셜로 가입해 로컬 비밀번호가 없는 계정(서버 1702 — BE `AuthService.login`).
                                // 자격 거절과 달리 입력을 고쳐서 될 일이 아니라 필드 인라인을 걸지 않는다.
                                // 문구를 여기서 직접 넣는 것은 사유별 매핑(`displayMessageResOrNull`)이 이
                                // 타입에 비밀번호 찾기 문구를 물려 두었기 때문이다 — 화면마다 안내가 다르다.
                                is CoreAuthFailure.SocialSignUpAccount -> {
                                    LoginReducerEvent.LoginFailed(UiText.Resource(R.string.onboarding_login_social_signup_account))
                                }

                                // 로그인 경로로 실제 오는 사유는 `SocialLoginRejected`(서버 1208·1209)와, 사유를
                                // 확인하지 못한 실패(null)뿐이다 — `AuthRepositoryImpl.mapLoginFailure` 가 만드는
                                // 나머지 셋(1201·1202 · 1702 · 네트워크)은 위에서 이미 갈렸다.
                                //
                                // 아래 넷은 여기까지 오지 않는다. `EmailAlreadyRegistered`·`EmailVerification`·
                                // `PasswordUnchanged` 는 계정 API 쪽 사유고(`AccountRepositoryImpl.mapAccountFailure`),
                                // BE 는 1206 을 비밀번호 변경·재설정(`AuthService.passwordChange`·`findPassword`)
                                // 에서만 던진다. `UserCancelledAuth` 는 소셜 SDK 를 부르는 `LoginScreen` 가 그 자리에서
                                // 걸러낸다 — `LoginUseCase` 는 토큰을 받은 뒤 서버만 친다. `else` 로 뭉개지 않고
                                // 열거해 두는 건 사유가 늘 때 컴파일러가 여기를 잡게 하려는 것이고, 도달하더라도
                                // 문구 매핑이 받아 주므로 안내가 비지 않는다.
                                is CoreAuthFailure.EmailAlreadyRegistered,
                                is CoreAuthFailure.EmailVerification,
                                is CoreAuthFailure.SocialLoginRejected,
                                is CoreAuthFailure.UserCancelledAuth,
                                is CoreAuthFailure.PasswordUnchanged,
                                null,
                                -> {
                                    LoginReducerEvent.LoginFailed(
                                        exception.toDisplayMessage(R.string.onboarding_login_failed),
                                    )
                                }
                            }
                        dispatch(event)
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
