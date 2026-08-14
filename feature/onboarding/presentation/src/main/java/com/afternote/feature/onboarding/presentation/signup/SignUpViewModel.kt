package com.afternote.feature.onboarding.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.EmailVerificationException
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.reporting.AuthFailureStage
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import com.afternote.feature.onboarding.presentation.reporting.recordAuthFailure
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel.Companion.RESEND_COOLDOWN_SECONDS
import com.afternote.feature.onboarding.presentation.toDisplayMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

/**
 * 회원가입 플로우 전체에서 공유되는 뷰모델.
 *
 * `Route.Onboarding` 그래프 스코프에 묶여 SignUp Step 1~4와 Profile 화면이 동일한 인스턴스를 공유.
 *
 * **상태 관리**: 모든 폼 필드 · 플래그 · 약관 · navigation 신호를 [SignUpUiState] 의 단일
 * `MutableStateFlow` 로 통합. UI 는 `collectAsStateWithLifecycle` 로 한 번에 구독.
 *
 * **TextFieldState 정책**: TextFieldState 는 각 Screen 이 `rememberTextFieldState` 로 소유하고,
 * ViewModel 은 평범한 `String` 으로 보관. Screen 이 LaunchedEffect + snapshotFlow 로 변경 사항을
 * VM 에 push, 다른 Screen 은 VM 의 [uiState] 에서 String 으로 read.
 */
@HiltViewModel
class SignUpViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val loginUseCase: LoginUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        companion object {
            /** "재전송" 클릭 후 다음 요청까지 강제 대기 초. 서버 비용 · SMS 발송량 보호. */
            private const val RESEND_COOLDOWN_SECONDS = 30
        }

        private val _uiState = MutableStateFlow(SignUpUiState())
        val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

        private var cooldownJob: Job? = null

        /**
         * 진행 중인 최종 제출. 상태 플래그만으로는 연타를 막지 못한다 — 두 호출이 모두
         * `isLoading = false` 를 읽고 통과한 뒤 각자 요청을 시작할 수 있다.
         */
        private var signUpJob: Job? = null

        // ─── 입력 reducer ───
        // 이메일이 바뀌면 앞서 받은 인증 에러는 더 이상 그 이메일의 것이 아니다.
        fun updateEmail(value: String) = _uiState.update { it.copy(email = value, hasVerificationError = false) }

        // photo picker 결과는 Entry 가 Uri.toString() 으로 변환해 push — VM 은 String 만 보관 (Framework Uri 의존 회피).
        fun onProfileImagePicked(uri: String?) = _uiState.update { it.copy(profileImageUri = uri) }

        fun updateVerificationCode(value: String) = _uiState.update { it.copy(verificationCode = value, hasVerificationError = false) }

        fun updateResidentFrontNumber(value: String) = _uiState.update { it.copy(residentFrontNumber = value) }

        fun updateResidentBackNumber(value: String) = _uiState.update { it.copy(residentBackNumber = value) }

        fun updateSignUpPassword(value: String) = _uiState.update { it.copy(signUpPassword = value) }

        fun updateSignUpPasswordConfirm(value: String) = _uiState.update { it.copy(signUpPasswordConfirm = value) }

        fun updateName(value: String) = _uiState.update { it.copy(name = value) }

        // ─── 단발성 신호 consume (UI 가 소비 후 호출) ───
        fun onSignedUpConsumed() = _uiState.update { it.copy(isSignedUp = false) }

        fun onResidentNumberNavigatedConsumed() = _uiState.update { it.copy(shouldNavigateToResidentNumber = false) }

        fun onNameRequiredConsumed() = _uiState.update { it.copy(isNameRequired = false) }

        fun onErrorConsumed() = _uiState.update { it.copy(errorMessage = null) }

        // ─── 약관 ───
        fun toggleTermsAgreed(agreed: Boolean) =
            _uiState.update {
                it.copy(termsState = it.termsState.copy(isTermsAgreed = agreed))
            }

        fun togglePrivacyAgreed(agreed: Boolean) =
            _uiState.update {
                it.copy(termsState = it.termsState.copy(isPrivacyAgreed = agreed))
            }

        fun toggleMarketingAgreed(agreed: Boolean) =
            _uiState.update {
                it.copy(termsState = it.termsState.copy(isMarketingAgreed = agreed))
            }

        fun toggleAllTerms(allAgreed: Boolean) =
            _uiState.update {
                it.copy(
                    termsState =
                        it.termsState.copy(
                            isTermsAgreed = allAgreed,
                            isPrivacyAgreed = allAgreed,
                            isMarketingAgreed = allAgreed,
                        ),
                )
            }

        // ─── 액션 ───
        fun requestVerification() {
            val state = _uiState.value
            if (state.isSendingCode || state.resendCooldownSeconds > 0) return
            viewModelScope.launch {
                _uiState.update { it.copy(isSendingCode = true) }
                accountRepository
                    .sendEmailCode(state.email)
                    .onSuccess {
                        _uiState.update { it.copy(isVerificationSent = true, hasVerificationError = false) }
                        startResendCooldown()
                    }.onFailure { error ->
                        // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                        if (error is CancellationException) throw error
                        errorReporter.recordAuthFailure(AuthFailureStage.EMAIL_CODE_SEND, error)
                        _uiState.update { it.copy(errorMessage = error.toDisplayMessage(R.string.signup_code_send_failed)) }
                    }
                _uiState.update { it.copy(isSendingCode = false) }
            }
        }

        /** 인증번호 발송 성공 직후 호출. [RESEND_COOLDOWN_SECONDS] 동안 카운트다운하며 재전송 연타 차단. */
        private fun startResendCooldown() {
            cooldownJob?.cancel()
            cooldownJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(resendCooldownSeconds = RESEND_COOLDOWN_SECONDS) }
                    while (_uiState.value.resendCooldownSeconds > 0) {
                        delay(1000.milliseconds)
                        _uiState.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
                    }
                }
        }

        /**
         * Step 1 "다음" 클릭 시점에 호출.
         * 이메일/인증번호를 서버에 검증해 성공 시 [SignUpUiState.shouldNavigateToResidentNumber] = true,
         * 인증번호 무효([EmailVerificationException] — 불일치/만료/미존재)는
         * [SignUpUiState.hasVerificationError] (인라인 문구), 그 외 실패는
         * [SignUpUiState.errorMessage] (스낵바) 로 set. 만료 판정은 서버가 한다.
         */
        fun verifyEmailAndProceed() {
            val state = _uiState.value
            if (state.isVerifyingEmail) return
            viewModelScope.launch {
                _uiState.update { it.copy(isVerifyingEmail = true, hasVerificationError = false) }
                accountRepository
                    .verifyEmail(
                        email = state.email,
                        certificateCode = state.verificationCode,
                    ).onSuccess {
                        _uiState.update { it.copy(shouldNavigateToResidentNumber = true) }
                    }.onFailure { error ->
                        // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                        if (error is CancellationException) throw error
                        if (error is EmailVerificationException) {
                            // 표시 문구는 화면의 고정 리소스 — 이 값은 인라인 표시 트리거 + 디버깅용 원문.
                            // 인증번호 불일치·만료는 정상적인 사용자 입력 오류라 리포팅하지 않는다.
                            // 스낵바 신호를 함께 내린다 — 이번 실패는 인라인으로 알리므로,
                            // 아직 소비되지 않은 이전 실패 문구가 인라인과 겹쳐 뜨지 않게 한다.
                            _uiState.update { it.copy(hasVerificationError = true, errorMessage = null) }
                        } else {
                            errorReporter.recordAuthFailure(AuthFailureStage.EMAIL_VERIFY, error)
                            _uiState.update { it.copy(errorMessage = error.toDisplayMessage(R.string.signup_email_verify_failed)) }
                        }
                    }
                _uiState.update { it.copy(isVerifyingEmail = false) }
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
        fun submitSignUp() {
            if (signUpJob?.isActive == true) return

            val state = _uiState.value
            val trimmedName = state.name.trim()
            if (trimmedName.isEmpty()) {
                _uiState.update { it.copy(isNameRequired = true) }
                return
            }

            signUpJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    try {
                        if (!state.isAccountCreated) {
                            accountRepository
                                .signUp(
                                    email = state.email,
                                    password = state.signUpPassword,
                                    name = trimmedName,
                                    profileUrl = state.profileImageUri,
                                ).onSuccess {
                                    _uiState.update { it.copy(isAccountCreated = true) }
                                }.onFailure { error ->
                                    // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                                    if (error is CancellationException) throw error
                                    errorReporter.recordAuthFailure(AuthFailureStage.SIGN_UP, error)
                                    _uiState.update { it.copy(errorMessage = error.toDisplayMessage(R.string.signup_failed)) }
                                    return@launch
                                }
                        }

                        loginUseCase(LoginType.Email(email = state.email, password = state.signUpPassword))
                            .onSuccess {
                                _uiState.update { it.copy(isSignedUp = true) }
                            }.onFailure { error ->
                                // 취소는 장애가 아니다 — 기록·UI 소비 전에 되던져 전파를 보존한다(전수 정정은 #661).
                                if (error is CancellationException) throw error
                                errorReporter.recordAuthFailure(
                                    stage = AuthFailureStage.AUTO_LOGIN_AFTER_SIGN_UP,
                                    throwable = error,
                                    provider = AuthProvider.EMAIL,
                                )
                                _uiState.update {
                                    it.copy(
                                        errorMessage = error.toDisplayMessage(R.string.signup_auto_login_failed),
                                    )
                                }
                            }
                    } finally {
                        // 어느 갈래로 빠져나가든 버튼 잠금은 풀어야 한다.
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }
