package com.afternote.feature.onboarding.presentation.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel.Companion.RESEND_COOLDOWN_SECONDS
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel.Companion.VERIFICATION_CODE_TTL_SECONDS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 회원가입 플로우 전체에서 공유되는 뷰모델.
 *
 * `Route.Onboarding` 그래프 스코프에 묶여 SignUp Step 1~4와 Profile 화면이 동일한 인스턴스를 공유.
 *
 * **상태 관리**: 모든 폼 필드 · 플래그 · 약관 · navigation 신호를 [SignUpUiState] 의 단일
 * `MutableStateFlow` 로 통합. UI 는 `collectAsStateWithLifecycle` 로 한 번에 구독.
 *
 * **TextFieldState 정책**: TextFieldState 는 각 Screen 이 `rememberTextFieldState` 로 소유하고,
 * ViewModel 은 평범한 [String] 으로 보관. Screen 이 LaunchedEffect + snapshotFlow 로 변경 사항을
 * VM 에 push, 다른 Screen 은 VM 의 [uiState] 에서 String 으로 read.
 */
@HiltViewModel
class SignUpViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val loginUseCase: LoginUseCase,
    ) : ViewModel() {
        companion object {
            /** "재전송" 클릭 후 다음 요청까지 강제 대기 초. 서버 비용 · SMS 발송량 보호. */
            private const val RESEND_COOLDOWN_SECONDS = 30

            /**
             * 인증번호 만료까지 남은 초. 백엔드 [EmailService.java](https://github.com/Afternote/Afternote-BE/blob/main/src/main/java/com/afternote/domain/auth/service/EmailService.java)
             * 의 Redis TTL 과 일치 (`set(..., 3, TimeUnit.MINUTES)`). 메일 본문도 "3분 안에 입력해주세요" 안내.
             */
            private const val VERIFICATION_CODE_TTL_SECONDS = 180
        }

        private val _uiState = MutableStateFlow(SignUpUiState())
        val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

        private var cooldownJob: Job? = null
        private var expiryJob: Job? = null

        // ─── 입력 reducer ───
        fun updateEmail(value: String) = _uiState.update { it.copy(email = value) }

        // photo picker 결과는 Entry 가 Uri.toString() 으로 변환해 push — VM 은 String 만 보관 (Framework Uri 의존 회피).
        fun onProfileImagePicked(uri: String?) = _uiState.update { it.copy(profileImageUri = uri) }

        fun updateVerificationCode(value: String) = _uiState.update { it.copy(verificationCode = value) }

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
                        _uiState.update { it.copy(isVerificationSent = true) }
                        startResendCooldown()
                        startExpiryCountdown()
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
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
                        delay(1000)
                        _uiState.update { it.copy(resendCooldownSeconds = it.resendCooldownSeconds - 1) }
                    }
                }
        }

        /**
         * 인증번호 발송 성공 직후 호출. [VERIFICATION_CODE_TTL_SECONDS] 부터 1초 틱으로 감소.
         * 재전송 시 이전 카운트다운은 취소되고 새로 시작 — 마지막 발송 시점 기준 TTL.
         */
        private fun startExpiryCountdown() {
            expiryJob?.cancel()
            expiryJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(verificationRemainingSeconds = VERIFICATION_CODE_TTL_SECONDS) }
                    while (_uiState.value.verificationRemainingSeconds > 0) {
                        delay(1000)
                        _uiState.update { it.copy(verificationRemainingSeconds = it.verificationRemainingSeconds - 1) }
                    }
                }
        }

        /**
         * Step 1 "다음" 클릭 시점에 호출.
         * 이메일/인증번호를 서버에 검증해 성공 시 [SignUpUiState.shouldNavigateToResidentNumber] = true 로 set,
         * 실패/거부 시 [SignUpUiState.errorMessage] 로 set.
         */
        fun verifyEmailAndProceed() {
            val state = _uiState.value
            if (state.isVerifyingEmail) return
            viewModelScope.launch {
                _uiState.update { it.copy(isVerifyingEmail = true) }
                accountRepository
                    .verifyEmail(
                        email = state.email,
                        certificateCode = state.verificationCode,
                    ).onSuccess {
                        _uiState.update { it.copy(shouldNavigateToResidentNumber = true) }
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message ?: "이메일 인증 실패") }
                    }
                _uiState.update { it.copy(isVerifyingEmail = false) }
            }
        }

        /**
         * 최종 회원가입 제출.
         * 1~4단계와 프로필에서 수집한 데이터를 취합하여 서버에 전송.
         * 회원가입 API 는 토큰을 내려주지 않으므로 같은 자격증명으로 자동 로그인.
         */
        fun submitSignUp() {
            val state = _uiState.value
            viewModelScope.launch {
                if (state.isLoading) return@launch

                val trimmedName = state.name.trim()
                if (trimmedName.isEmpty()) {
                    _uiState.update { it.copy(isNameRequired = true) }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true) }
                accountRepository
                    .signUp(
                        email = state.email,
                        password = state.signUpPassword,
                        name = trimmedName,
                        profileUrl = state.profileImageUri,
                    ).onSuccess {
                        loginUseCase(LoginType.Email(email = state.email, password = state.signUpPassword))
                            .onSuccess {
                                _uiState.update { it.copy(isSignedUp = true) }
                            }.onFailure { error ->
                                _uiState.update {
                                    it.copy(
                                        errorMessage = error.message ?: "자동 로그인에 실패했어요. 로그인 화면에서 다시 시도해주세요.",
                                    )
                                }
                            }
                    }.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.message) }
                    }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
