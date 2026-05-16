package com.afternote.feature.onboarding.presentation.signup

import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.text.input.TextFieldState
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
        }

        private val eventChannel = Channel<SignUpEvent>(Channel.BUFFERED)
        val eventFlow: Flow<SignUpEvent> = eventChannel.receiveAsFlow()

        // Step 1: 이메일 & 인증번호
        val emailState = TextFieldState()
        val verificationCodeState = TextFieldState()
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

        // Step 2: 주민등록번호
        val frontNumberState = TextFieldState()
        val backNumberState = TextFieldState()

        // Step 3: 비밀번호 설정
        val signUpPasswordState = TextFieldState()
        val signUpPasswordConfirmState = TextFieldState()

        // Step 4: 약관 동의
        var termsState by mutableStateOf(TermsState())
            private set

        // Profile
        val nameState = TextFieldState()

        init {
            // TODO(fix/141): 이름 빈값 전송 재현 후 제거.
            Log.d(
                "SignUp/debug",
                "VM created: vm=${System.identityHashCode(this)}, nameState=${System.identityHashCode(nameState)}",
            )
        }

        private val _profileImageUri = MutableStateFlow<Uri?>(null)
        val profileImageUri: StateFlow<Uri?> = _profileImageUri.asStateFlow()

        fun onProfileImagePicked(uri: Uri?) {
            _profileImageUri.value = uri
        }

        // UI 상태
        var isLoading by mutableStateOf(false)
            private set

        /** 이메일 형식 유효성. "인증번호 받기" / "다음" 활성화 조건의 사전 가드. */
        val isEmailFormatValid by derivedStateOf {
            val email = emailState.text.toString()
            email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        }

        /** Step 1 — 이메일·인증번호 입력 후 다음 단계 진행 가능 여부 */
        val isStep1NextEnabled by derivedStateOf {
            !isVerifyingEmail &&
                isEmailFormatValid &&
                verificationCodeState.text.length >= MIN_VERIFICATION_CODE_LENGTH
        }

        /** Step 2 — 주민등록번호 앞 6자리 + 뒷 첫 1자리 */
        val isStep2NextEnabled by derivedStateOf {
            frontNumberState.text.length == RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT &&
                backNumberState.text.length == RESIDENT_REGISTRATION_BACK_FIRST_DIGIT_COUNT
        }

        /** 비밀번호 정규식 충족 여부. 안내 문구 색상 토글에도 사용. */
        val isPasswordRuleSatisfied by derivedStateOf {
            PASSWORD_REGEX.matches(signUpPasswordState.text.toString())
        }

        /** Step 3 — 비밀번호 규칙 충족 + 확인 일치 */
        val isStep3NextEnabled by derivedStateOf {
            isPasswordRuleSatisfied &&
                signUpPasswordState.text.toString() == signUpPasswordConfirmState.text.toString()
        }

        /** Step 4 — 필수 약관(이용·개인정보) 동의 */
        val isStep4NextEnabled by derivedStateOf {
            termsState.isTermsAgreed && termsState.isPrivacyAgreed
        }

        fun requestVerification() {
            if (isSendingCode || resendCooldownSeconds > 0) return
            viewModelScope.launch {
                isSendingCode = true
                accountRepository
                    .sendEmailCode(emailState.text.toString())
                    .onSuccess {
                        isVerificationSent = true
                        startResendCooldown()
                    }.onFailure { error ->
                        eventChannel.send(
                            SignUpEvent.ShowError(error.message ?: "인증번호 요청 실패"),
                        )
                    }
                isSendingCode = false
            }
        }

        /** 인증번호 발송 성공 직후 호출. 60초 카운트다운을 시작해 재전송 연타를 막는다. */
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
                        email = emailState.text.toString(),
                        certificateCode = verificationCodeState.text.toString(),
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

                val rawName = nameState.text.toString()
                // TODO(fix/141): 이름 빈값 전송 재현 후 제거.
                Log.d(
                    "SignUp/debug",
                    "submitSignUp: vm=${System.identityHashCode(this@SignUpViewModel)}, " +
                        "nameState=${System.identityHashCode(nameState)}, " +
                        "text='$rawName', length=${rawName.length}",
                )

                val name = rawName.trim()
                if (name.isEmpty()) {
                    eventChannel.send(SignUpEvent.ShowError("이름을 입력해주세요"))
                    return@launch
                }

                isLoading = true
                val email = emailState.text.toString()
                val password = signUpPasswordState.text.toString()
                accountRepository
                    .signUp(
                        email = email,
                        password = password,
                        name = name,
                        profileUrl = _profileImageUri.value?.toString(),
                    ).onSuccess {
                        // 회원가입 API 는 토큰을 내려주지 않으므로 같은 자격증명으로 자동 로그인.
                        loginUseCase(LoginType.Email(email = email, password = password))
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
                            SignUpEvent.ShowError(error.message ?: "회원가입 실패"),
                        )
                    }
                isLoading = false
            }
        }
    }
