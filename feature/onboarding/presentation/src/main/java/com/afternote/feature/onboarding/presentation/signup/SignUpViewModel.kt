package com.afternote.feature.onboarding.presentation.signup

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.reporting.AuthFailureStage
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import com.afternote.feature.onboarding.presentation.reporting.recordAuthFailure
import com.afternote.feature.onboarding.presentation.toDisplayMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * 회원가입 플로우 전체에서 공유되는 뷰모델.
 *
 * `Route.Onboarding` 그래프 스코프에 묶여 SignUp Step 1~4와 Profile 화면이 동일한 인스턴스를 공유.
 *
 * **상태 관리**: 모든 폼 필드 · 플래그 · 약관 · navigation 신호를 [SignUpUiState] 하나로 통합하고,
 * 전이는 [reduce] 한 곳에서만 일어난다. 화면은 [onIntent] 하나로 들어온다 (#1802).
 *
 * **TextFieldState 정책**: TextFieldState 는 각 Screen 이 `rememberTextFieldState` 로 소유하고,
 * ViewModel 은 평범한 `String` 으로 보관. Screen 이 LaunchedEffect + snapshotFlow 로 변경 사항을
 * Intent 로 push, 다른 Screen 은 [uiState] 에서 String 으로 read.
 */
@HiltViewModel
class SignUpViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val loginUseCase: LoginUseCase,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<SignUpIntent, SignUpUiState, SignUpReducerEvent>(SignUpUiState()) {
        private var cooldownJob: Job? = null

        /**
         * 진행 중인 최종 제출. 상태 플래그만으로는 연타를 막지 못한다 — 두 호출이 모두
         * `isLoading = false` 를 읽고 통과한 뒤 각자 요청을 시작할 수 있다.
         */
        private var signUpJob: Job? = null

        override fun onIntent(intent: SignUpIntent) {
            when (intent) {
                is SignUpIntent.UpdateEmail -> dispatch(SignUpReducerEvent.EmailChanged(intent.value))
                is SignUpIntent.UpdateVerificationCode -> dispatch(SignUpReducerEvent.VerificationCodeChanged(intent.value))
                is SignUpIntent.UpdateResidentFrontNumber -> dispatch(SignUpReducerEvent.ResidentFrontNumberChanged(intent.value))
                is SignUpIntent.UpdateResidentBackNumber -> dispatch(SignUpReducerEvent.ResidentBackNumberChanged(intent.value))
                is SignUpIntent.UpdateSignUpPassword -> dispatch(SignUpReducerEvent.SignUpPasswordChanged(intent.value))
                is SignUpIntent.UpdateSignUpPasswordConfirm -> dispatch(SignUpReducerEvent.SignUpPasswordConfirmChanged(intent.value))
                is SignUpIntent.UpdateName -> dispatch(SignUpReducerEvent.NameChanged(intent.value))
                is SignUpIntent.PickProfileImage -> dispatch(SignUpReducerEvent.ProfileImagePicked(intent.uri))
                is SignUpIntent.ToggleTermsAgreed -> dispatch(SignUpReducerEvent.TermsAgreementChanged(intent.agreed))
                is SignUpIntent.TogglePrivacyAgreed -> dispatch(SignUpReducerEvent.PrivacyAgreementChanged(intent.agreed))
                is SignUpIntent.ToggleMarketingAgreed -> dispatch(SignUpReducerEvent.MarketingAgreementChanged(intent.agreed))
                is SignUpIntent.ToggleAllTerms -> dispatch(SignUpReducerEvent.AllAgreementsChanged(intent.agreed))
                SignUpIntent.RequestVerification -> requestVerification()
                SignUpIntent.VerifyEmailAndProceed -> verifyEmailAndProceed()
                SignUpIntent.SubmitSignUp -> submitSignUp()
                SignUpIntent.ConsumeSignedUp -> dispatch(SignUpReducerEvent.SignedUpConsumed)
                SignUpIntent.ConsumeResidentNumberNavigation -> dispatch(SignUpReducerEvent.ResidentNumberNavigationConsumed)
                SignUpIntent.ConsumeNameRequired -> dispatch(SignUpReducerEvent.NameRequiredConsumed)
                SignUpIntent.ConsumeError -> dispatch(SignUpReducerEvent.ErrorConsumed)
            }
        }

        override fun reduce(
            state: SignUpUiState,
            event: SignUpReducerEvent,
        ): SignUpUiState =
            when (event) {
                // 이메일이 바뀌면 앞서 받은 인증 에러는 더 이상 그 이메일의 것이 아니다.
                is SignUpReducerEvent.EmailChanged -> {
                    state.copy(email = event.value, hasVerificationError = false)
                }

                is SignUpReducerEvent.VerificationCodeChanged -> {
                    state.copy(verificationCode = event.value, hasVerificationError = false)
                }

                is SignUpReducerEvent.ResidentFrontNumberChanged -> {
                    state.copy(residentFrontNumber = event.value)
                }

                is SignUpReducerEvent.ResidentBackNumberChanged -> {
                    state.copy(residentBackNumber = event.value)
                }

                is SignUpReducerEvent.SignUpPasswordChanged -> {
                    state.copy(signUpPassword = event.value)
                }

                is SignUpReducerEvent.SignUpPasswordConfirmChanged -> {
                    state.copy(signUpPasswordConfirm = event.value)
                }

                is SignUpReducerEvent.NameChanged -> {
                    state.copy(name = event.value)
                }

                is SignUpReducerEvent.ProfileImagePicked -> {
                    state.copy(profileImageUri = event.uri)
                }

                is SignUpReducerEvent.TermsAgreementChanged -> {
                    state.copy(termsState = state.termsState.copy(isTermsAgreed = event.agreed))
                }

                is SignUpReducerEvent.PrivacyAgreementChanged -> {
                    state.copy(termsState = state.termsState.copy(isPrivacyAgreed = event.agreed))
                }

                is SignUpReducerEvent.MarketingAgreementChanged -> {
                    state.copy(termsState = state.termsState.copy(isMarketingAgreed = event.agreed))
                }

                is SignUpReducerEvent.AllAgreementsChanged -> {
                    state.copy(
                        termsState =
                            state.termsState.copy(
                                isTermsAgreed = event.agreed,
                                isPrivacyAgreed = event.agreed,
                                isMarketingAgreed = event.agreed,
                            ),
                    )
                }

                SignUpReducerEvent.CodeSendStarted -> {
                    state.copy(isSendingCode = true)
                }

                SignUpReducerEvent.CodeSent -> {
                    state.copy(isVerificationSent = true, hasVerificationError = false)
                }

                is SignUpReducerEvent.CodeSendFailed -> {
                    state.copy(errorMessage = event.message)
                }

                SignUpReducerEvent.CodeSendFinished -> {
                    state.copy(isSendingCode = false)
                }

                SignUpReducerEvent.CooldownReloaded -> {
                    state.copy(resendCooldownSeconds = RESEND_COOLDOWN_SECONDS)
                }

                SignUpReducerEvent.CooldownTicked -> {
                    state.copy(resendCooldownSeconds = state.resendCooldownSeconds - 1)
                }

                SignUpReducerEvent.EmailVerifyStarted -> {
                    state.copy(isVerifyingEmail = true, hasVerificationError = false)
                }

                SignUpReducerEvent.EmailVerified -> {
                    state.copy(shouldNavigateToResidentNumber = true)
                }

                // 스낵바 신호를 함께 내린다 — 이번 실패는 인라인으로 알리므로, 아직 소비되지 않은
                // 이전 실패 문구가 인라인과 겹쳐 뜨지 않게 한다.
                SignUpReducerEvent.VerificationRejected -> {
                    state.copy(hasVerificationError = true, errorMessage = null)
                }

                is SignUpReducerEvent.EmailVerifyFailed -> {
                    state.copy(errorMessage = event.message)
                }

                SignUpReducerEvent.EmailVerifyFinished -> {
                    state.copy(isVerifyingEmail = false)
                }

                SignUpReducerEvent.NameRequired -> {
                    state.copy(isNameRequired = true)
                }

                SignUpReducerEvent.SubmitStarted -> {
                    state.copy(isLoading = true)
                }

                SignUpReducerEvent.AccountCreated -> {
                    state.copy(isAccountCreated = true)
                }

                is SignUpReducerEvent.SubmitFailed -> {
                    state.copy(errorMessage = event.message)
                }

                SignUpReducerEvent.SignedUp -> {
                    state.copy(isSignedUp = true)
                }

                SignUpReducerEvent.SubmitFinished -> {
                    state.copy(isLoading = false)
                }

                SignUpReducerEvent.SignedUpConsumed -> {
                    state.copy(isSignedUp = false)
                }

                SignUpReducerEvent.ResidentNumberNavigationConsumed -> {
                    state.copy(shouldNavigateToResidentNumber = false)
                }

                SignUpReducerEvent.NameRequiredConsumed -> {
                    state.copy(isNameRequired = false)
                }

                SignUpReducerEvent.ErrorConsumed -> {
                    state.copy(errorMessage = null)
                }
            }

        private fun requestVerification() {
            val state = currentState
            if (state.isSendingCode || state.resendCooldownSeconds > 0) return
            viewModelScope.launch {
                dispatch(SignUpReducerEvent.CodeSendStarted)
                accountRepository
                    .sendEmailCode(state.email)
                    .onSuccess {
                        dispatch(SignUpReducerEvent.CodeSent)
                        startResendCooldown()
                    }.onFailure { error ->
                        // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                        if (error is CancellationException) throw error
                        errorReporter.recordAuthFailure(AuthFailureStage.EMAIL_CODE_SEND, error)
                        dispatch(
                            SignUpReducerEvent.CodeSendFailed(
                                error.toDisplayMessage(R.string.onboarding_signup_code_send_failed),
                            ),
                        )
                    }
                dispatch(SignUpReducerEvent.CodeSendFinished)
            }
        }

        /** 인증번호 발송 성공 직후 호출. [RESEND_COOLDOWN_SECONDS] 동안 카운트다운하며 재전송 연타 차단. */
        private fun startResendCooldown() {
            cooldownJob?.cancel()
            cooldownJob =
                viewModelScope.launch {
                    dispatch(SignUpReducerEvent.CooldownReloaded)
                    while (currentState.resendCooldownSeconds > 0) {
                        delay(MILLIS_PER_SECOND.milliseconds)
                        dispatch(SignUpReducerEvent.CooldownTicked)
                    }
                }
        }

        /**
         * Step 1 "다음" 클릭 시점에 호출.
         * 이메일/인증번호를 서버에 검증해 성공 시 [SignUpUiState.shouldNavigateToResidentNumber] = true,
         * 인증번호 무효([CoreAuthFailure.EmailVerification] — 불일치/만료/미존재)는
         * [SignUpUiState.hasVerificationError] (인라인 문구), 그 외 실패는
         * [SignUpUiState.errorMessage] (스낵바) 로 set. 만료 판정은 서버가 한다.
         */
        private fun verifyEmailAndProceed() {
            val state = currentState
            if (state.isVerifyingEmail) return
            viewModelScope.launch {
                dispatch(SignUpReducerEvent.EmailVerifyStarted)
                accountRepository
                    .verifyEmail(
                        email = state.email,
                        certificateCode = state.verificationCode,
                    ).onSuccess {
                        dispatch(SignUpReducerEvent.EmailVerified)
                    }.onFailure { error ->
                        // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                        if (error is CancellationException) throw error
                        if (error is CoreAuthFailure.EmailVerification) {
                            // 인증번호 불일치·만료는 정상적인 사용자 입력 오류라 리포팅하지 않는다.
                            dispatch(SignUpReducerEvent.VerificationRejected)
                        } else {
                            errorReporter.recordAuthFailure(AuthFailureStage.EMAIL_VERIFY, error)
                            dispatch(
                                SignUpReducerEvent.EmailVerifyFailed(
                                    error.toDisplayMessage(R.string.onboarding_signup_email_verify_failed),
                                ),
                            )
                        }
                    }
                dispatch(SignUpReducerEvent.EmailVerifyFinished)
            }
        }

        /**
         * 최종 회원가입 제출.
         * 1~4단계와 프로필에서 수집한 데이터를 취합하여 서버에 전송.
         * 회원가입 API 는 토큰을 내려주지 않으므로 같은 자격증명으로 자동 로그인.
         *
         * 가입과 자동 로그인은 따로 성공할 수 있어 재시도 지점이 다르다
         * ([SignUpUiState.isAccountCreated]).
         */
        private fun submitSignUp() {
            if (signUpJob?.isActive == true) return

            val state = currentState
            val trimmedName = state.name.trim()
            if (trimmedName.isEmpty()) {
                dispatch(SignUpReducerEvent.NameRequired)
                return
            }

            signUpJob =
                viewModelScope.launch {
                    dispatch(SignUpReducerEvent.SubmitStarted)
                    try {
                        if (!state.isAccountCreated) {
                            accountRepository
                                .signUp(
                                    email = state.email,
                                    password = state.signUpPassword,
                                    name = trimmedName,
                                    profileUrl = state.profileImageUri,
                                ).onSuccess {
                                    dispatch(SignUpReducerEvent.AccountCreated)
                                }.onFailure { error ->
                                    // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                                    if (error is CancellationException) throw error
                                    errorReporter.recordAuthFailure(AuthFailureStage.SIGN_UP, error)
                                    dispatch(
                                        SignUpReducerEvent.SubmitFailed(
                                            error.toDisplayMessage(R.string.onboarding_signup_failed),
                                        ),
                                    )
                                    return@launch
                                }
                        }

                        loginUseCase(LoginType.Email(email = state.email, password = state.signUpPassword))
                            .onSuccess {
                                dispatch(SignUpReducerEvent.SignedUp)
                            }.onFailure { error ->
                                // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                                if (error is CancellationException) throw error
                                errorReporter.recordAuthFailure(
                                    stage = AuthFailureStage.AUTO_LOGIN_AFTER_SIGN_UP,
                                    throwable = error,
                                    provider = AuthProvider.EMAIL,
                                )
                                dispatch(
                                    SignUpReducerEvent.SubmitFailed(
                                        error.toDisplayMessage(R.string.onboarding_signup_auto_login_failed),
                                    ),
                                )
                            }
                    } finally {
                        // 어느 갈래로 빠져나가든 버튼 잠금은 풀어야 한다.
                        dispatch(SignUpReducerEvent.SubmitFinished)
                    }
                }
        }

        private companion object {
            /** "재전송" 클릭 후 다음 요청까지 강제 대기 초. 서버 비용 · SMS 발송량 보호. */
            const val RESEND_COOLDOWN_SECONDS = 30
            const val MILLIS_PER_SECOND = 1000L
        }
    }
