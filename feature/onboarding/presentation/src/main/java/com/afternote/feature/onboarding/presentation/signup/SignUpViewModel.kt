package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel.Companion.RESEND_COOLDOWN_SECONDS
import com.afternote.feature.onboarding.presentation.terms.TermsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 회원가입 플로우 전체에서 공유되는 뷰모델.
 *
 * `Route.Onboarding` 그래프 스코프에 묶여 SignUp Step 1~4와 Profile 화면이
 * 동일한 인스턴스를 공유합니다.
 *
 * **UI 상태 보관 정책**: TextFieldState 는 각 Screen 이 [rememberTextFieldState] 로
 * 소유하고, ViewModel 은 평범한 [String] 으로 보관합니다. Screen 이 LaunchedEffect +
 * snapshotFlow 로 변경 사항을 VM 에 push, 다른 Screen 은 VM 의 String 으로 read.
 */
@HiltViewModel
class SignUpViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
        private val loginUseCase: LoginUseCase,
    ) : ViewModel() {
        companion object {
            /** 주민등록번호 앞자리(생년월일) 자릿수 */
            const val RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT = 6

            /** 뒷자리 UI에서 수집하는 첫 번째 마스킹 전 숫자 1자리 */
            const val RESIDENT_REGISTRATION_BACK_FIRST_DIGIT_COUNT = 1

            private const val MIN_VERIFICATION_CODE_LENGTH = 6

            /** "재전송" 클릭 후 다음 요청까지 강제 대기 초. 서버 비용·SMS 발송량 보호. */
            private const val RESEND_COOLDOWN_SECONDS = 30

            /** 8~16자, 영문 대소문자 + 숫자 + 특수문자 각 1개 이상. */
            private val PASSWORD_REGEX =
                Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$")

            // android.util.Patterns.EMAIL_ADDRESS 와 동일 — VM 의 framework 의존 제거 목적.
            // 원본: AOSP frameworks/base/core/java/android/util/Patterns.java
            private val EMAIL_ADDRESS_REGEX =
                Regex(
                    "[a-zA-Z0-9+._%\\-]{1,256}" +
                        "@" +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                        "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+",
                )
        }

        private val eventChannel = Channel<SignUpEvent>(Channel.BUFFERED)
        val eventFlow: Flow<SignUpEvent> = eventChannel.receiveAsFlow()

        // ─── 입력 값 (각 Screen 의 rememberTextFieldState 로부터 push) ───
        // Step 1
        var email: String by mutableStateOf("")
            private set
        var verificationCode: String by mutableStateOf("")
            private set

        // Step 2
        var residentFrontNumber: String by mutableStateOf("")
            private set
        var residentBackNumber: String by mutableStateOf("")
            private set

        // Step 3
        var signUpPassword: String by mutableStateOf("")
            private set
        var signUpPasswordConfirm: String by mutableStateOf("")
            private set

        // Profile
        var name: String by mutableStateOf("")
            private set

        fun updateEmail(value: String) {
            email = value
        }

        fun updateVerificationCode(value: String) {
            verificationCode = value
        }

        fun updateResidentFrontNumber(value: String) {
            residentFrontNumber = value
        }

        fun updateResidentBackNumber(value: String) {
            residentBackNumber = value
        }

        fun updateSignUpPassword(value: String) {
            signUpPassword = value
        }

        fun updateSignUpPasswordConfirm(value: String) {
            signUpPasswordConfirm = value
        }

        fun updateName(value: String) {
            name = value
        }

        // ─── 플래그 ───
        var isVerificationSent by mutableStateOf(false)
            private set

        /** 인증번호 전송 요청 진행 중. 버튼 중복 클릭 방지 + 로딩 텍스트 토글에 사용. */
        var isSendingCode by mutableStateOf(false)
            private set

        /** 이메일/인증번호 검증 요청 진행 중. Step 1 "다음" 중복 클릭 방지. */
        var isVerifyingEmail by mutableStateOf(false)
            private set

        /** 재전송 쿨다운 남은 초. 0 이면 즉시 재요청 가능. */
        var resendCooldownSeconds by mutableIntStateOf(0)
            private set

        private var cooldownJob: Job? = null

        // Step 4: 약관 동의
        var termsState by mutableStateOf(TermsState())
            private set

        // photo picker 결과는 Entry 가 Uri.toString() 으로 변환해 push — VM 은 String 만 보관.
        private val _profileImageUri = MutableStateFlow<String?>(null)
        val profileImageUri: StateFlow<String?> = _profileImageUri.asStateFlow()

        fun onProfileImagePicked(uri: String?) {
            _profileImageUri.value = uri
        }

        // UI 상태
        var isLoading by mutableStateOf(false)
            private set

        // ─── derivedStateOf ───

        /** 이메일 형식 유효성. "인증번호 받기" / "다음" 활성화 조건의 사전 가드. */
        val isEmailFormatValid by derivedStateOf {
            email.isNotBlank() && EMAIL_ADDRESS_REGEX.matches(email)
        }

        /** Step 1 — 이메일·인증번호 입력 후 다음 단계 진행 가능 여부 */
        val isStep1NextEnabled by derivedStateOf {
            !isVerifyingEmail &&
                isEmailFormatValid &&
                verificationCode.length >= MIN_VERIFICATION_CODE_LENGTH
        }

        /** Step 2 — 주민등록번호 앞 6자리 + 뒷 첫 1자리 */
        val isStep2NextEnabled by derivedStateOf {
            residentFrontNumber.length == RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT &&
                residentBackNumber.length == RESIDENT_REGISTRATION_BACK_FIRST_DIGIT_COUNT
        }

        /** 비밀번호 정규식 충족 여부. 안내 문구 색상 토글에도 사용. */
        val isPasswordRuleSatisfied by derivedStateOf {
            PASSWORD_REGEX.matches(signUpPassword)
        }

        /** Step 3 — 비밀번호 규칙 충족 + 확인 일치 */
        val isStep3NextEnabled by derivedStateOf {
            isPasswordRuleSatisfied && signUpPassword == signUpPasswordConfirm
        }

        /** Step 4 — 필수 약관(이용·개인정보) 동의 */
        val isStep4NextEnabled by derivedStateOf {
            termsState.isTermsAgreed && termsState.isPrivacyAgreed
        }

        // ─── 액션 ───
        fun requestVerification() {
            if (isSendingCode || resendCooldownSeconds > 0) return
            viewModelScope.launch {
                isSendingCode = true
                accountRepository
                    .sendEmailCode(email)
                    .onSuccess {
                        isVerificationSent = true
                        startResendCooldown()
                    }.onFailure { error ->
                        eventChannel.send(
                            SignUpEvent.ShowError(error.message),
                        )
                    }
                isSendingCode = false
            }
        }

        /** 인증번호 발송 성공 직후 호출. [RESEND_COOLDOWN_SECONDS] 동안 카운트다운하며 재전송 연타를 막는다. */
        private fun startResendCooldown() {
            cooldownJob?.cancel()
            cooldownJob =
                viewModelScope.launch {
                    resendCooldownSeconds = RESEND_COOLDOWN_SECONDS
                    while (resendCooldownSeconds > 0) {
                        delay(1000)
                        resendCooldownSeconds -= 1
                    }
                }
        }

        /**
         * Step 1 "다음" 클릭 시점에 호출.
         * 이메일/인증번호를 서버에 검증해 성공 시 [SignUpEvent.NavigateToResidentNumber] 를,
         * 실패/거부 시 [SignUpEvent.ShowError] 를 emit.
         */
        fun verifyEmailAndProceed() {
            if (isVerifyingEmail) return
            viewModelScope.launch {
                isVerifyingEmail = true
                accountRepository
                    .verifyEmail(
                        email = email,
                        certificateCode = verificationCode,
                    ).onSuccess { result ->
                        if (result.isVerified) {
                            eventChannel.send(SignUpEvent.NavigateToResidentNumber)
                        } else {
                            eventChannel.send(SignUpEvent.ShowError("인증번호가 일치하지 않습니다"))
                        }
                    }.onFailure { error ->
                        eventChannel.send(
                            SignUpEvent.ShowError(error.message ?: "이메일 인증 실패"),
                        )
                    }
                isVerifyingEmail = false
            }
        }

        fun toggleTermsAgreed(agreed: Boolean) {
            termsState = termsState.copy(isTermsAgreed = agreed)
        }

        fun togglePrivacyAgreed(agreed: Boolean) {
            termsState = termsState.copy(isPrivacyAgreed = agreed)
        }

        fun toggleMarketingAgreed(agreed: Boolean) {
            termsState = termsState.copy(isMarketingAgreed = agreed)
        }

        fun toggleAllTerms(allAgreed: Boolean) {
            termsState =
                termsState.copy(
                    isTermsAgreed = allAgreed,
                    isPrivacyAgreed = allAgreed,
                    isMarketingAgreed = allAgreed,
                )
        }

        /**
         * 최종 회원가입 제출.
         * 1~4단계와 프로필에서 수집한 데이터를 취합하여 서버에 전송합니다.
         */
        fun submitSignUp() {
            viewModelScope.launch {
                if (isLoading) return@launch

                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    eventChannel.send(SignUpEvent.NameRequired)
                    return@launch
                }

                isLoading = true
                accountRepository
                    .signUp(
                        email = email,
                        password = signUpPassword,
                        name = trimmedName,
                        profileUrl = _profileImageUri.value,
                    ).onSuccess {
                        // 회원가입 API 는 토큰을 내려주지 않으므로 같은 자격증명으로 자동 로그인.
                        loginUseCase(LoginType.Email(email = email, password = signUpPassword))
                            .onSuccess {
                                eventChannel.send(SignUpEvent.SignUpSuccess)
                            }.onFailure { error ->
                                eventChannel.send(
                                    SignUpEvent.ShowError(
                                        error.message ?: "자동 로그인에 실패했어요. 로그인 화면에서 다시 시도해주세요.",
                                    ),
                                )
                            }
                    }.onFailure { error ->
                        eventChannel.send(
                            SignUpEvent.ShowError(error.message),
                        )
                    }
                isLoading = false
            }
        }
    }
