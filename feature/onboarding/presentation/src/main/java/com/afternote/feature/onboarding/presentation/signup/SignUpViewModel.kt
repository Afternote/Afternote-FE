package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.onboarding.presentation.terms.TermsState
import dagger.hilt.android.lifecycle.HiltViewModel
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
        // private val signUpUseCase: SignUpUseCase,
    ) : ViewModel() {
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

        // UI 상태
        var isLoading by mutableStateOf(false)
            private set

        fun requestVerification() {
            // TODO: 이메일 인증 요청 로직
            isVerificationSent = true
        }

        fun updateTermsState(newState: TermsState) {
            termsState = newState
        }

        /**
         * 최종 회원가입 제출.
         * 1~4단계와 프로필에서 수집한 데이터를 취합하여 서버에 전송합니다.
         */
        fun submitSignUp(onSuccess: () -> Unit) {
            viewModelScope.launch {
                isLoading = true
                try {
                    // TODO: signUpUseCase(email, password, name, residentNumber, termsState)

                    onSuccess()
                } catch (_: Exception) {
                    // TODO: 에러 처리 로직
                } finally {
                    isLoading = false
                }
            }
        }
    }
