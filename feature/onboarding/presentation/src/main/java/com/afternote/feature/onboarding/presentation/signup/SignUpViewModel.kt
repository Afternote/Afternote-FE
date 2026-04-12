package com.afternote.feature.onboarding.presentation.signup

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.usecase.account.SendEmailCodeUseCase
import com.afternote.core.domain.usecase.account.SignUpUseCase
import com.afternote.feature.onboarding.presentation.terms.TermsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private val signUpUseCase: SignUpUseCase,
        private val sendEmailCodeUseCase: SendEmailCodeUseCase,
    ) : ViewModel() {
        companion object {
            /** 주민등록번호 앞자리(생년월일) 자릿수 */
            const val RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT = 6

            /** 뒷자리 UI에서 수집하는 첫 번째 마스킹 전 숫자 1자리 */
            const val RESIDENT_REGISTRATION_BACK_FIRST_DIGIT_COUNT = 1

            private const val MIN_ACCOUNT_PASSWORD_LENGTH = 8
        }

        // Step 1: 이메일 & 비밀번호
        val emailState = TextFieldState()
        val passwordState = TextFieldState()
        var isVerificationSent by mutableStateOf(false)
            private set

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

        private val _profileImageUri = MutableStateFlow<Uri?>(null)
        val profileImageUri: StateFlow<Uri?> = _profileImageUri.asStateFlow()

        fun onProfileImagePicked(uri: Uri?) {
            _profileImageUri.value = uri
        }

        // UI 상태
        var isLoading by mutableStateOf(false)
            private set

        /** Step 1 — 이메일·계정 비밀번호 입력 후 다음 단계 진행 가능 여부 */
        val isStep1NextEnabled by derivedStateOf {
            emailState.text.isNotBlank() &&
                passwordState.text.length >= MIN_ACCOUNT_PASSWORD_LENGTH
        }

        /** Step 2 — 주민등록번호 앞 6자리 + 뒷 첫 1자리 */
        val isStep2NextEnabled by derivedStateOf {
            frontNumberState.text.length == RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT &&
                backNumberState.text.length == RESIDENT_REGISTRATION_BACK_FIRST_DIGIT_COUNT
        }

        /** Step 3 — 서비스 비밀번호 설정(확인 일치) */
        val isStep3NextEnabled by derivedStateOf {
            signUpPasswordState.text.isNotEmpty() &&
                signUpPasswordState.text.toString() == signUpPasswordConfirmState.text.toString()
        }

        /** Step 4 — 필수 약관(이용·개인정보) 동의 */
        val isStep4NextEnabled by derivedStateOf {
            termsState.isTermsAgreed && termsState.isPrivacyAgreed
        }

        fun requestVerification() {
            viewModelScope.launch {
                sendEmailCodeUseCase(emailState.text.toString())
                    .onSuccess { isVerificationSent = true }
                    .onFailure {
                        // TODO: 에러 UI 처리
                    }
            }
        }

        fun updateTermsState(newState: TermsState) {
            termsState = newState
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
        fun submitSignUp(onSuccess: () -> Unit) {
            viewModelScope.launch {
                if (isLoading) return@launch

                isLoading = true
                signUpUseCase(
                    email = emailState.text.toString(),
                    password = passwordState.text.toString(),
                    name = nameState.text.toString(),
                    profileUrl = _profileImageUri.value?.toString(),
                ).onSuccess {
                    onSuccess()
                }.onFailure {
                    // TODO: 에러 UI 처리
                }
                isLoading = false
            }
        }
    }
