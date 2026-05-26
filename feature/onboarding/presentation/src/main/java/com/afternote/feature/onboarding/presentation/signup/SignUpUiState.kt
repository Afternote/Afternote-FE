package com.afternote.feature.onboarding.presentation.signup

import android.net.Uri
import android.util.Patterns
import com.afternote.feature.onboarding.presentation.terms.TermsState

/**
 * 회원가입 플로우 전체에서 공유되는 단일 UI 상태.
 *
 * Step 1~4 + Profile 화면이 동일 인스턴스 (`Route.Onboarding` 그래프 스코프) 의
 * [SignUpViewModel] 의 `MutableStateFlow<SignUpUiState>` 를 구독한다.
 *
 * 입력값 · 플래그 · 약관 · navigation 신호를 한 인스턴스로 묶어 reducer (`_uiState.update { copy(...) }`)
 * 로 갱신. 단발성 navigation/error 신호는 UI 가 소비 후 [SignUpViewModel] 의 `onXxxConsumed()`
 * 콜백 호출로 reset.
 *
 * 폼 룰 (const + regex) 은 본 data class 의 companion 으로 묶어 ViewModel + UI 양쪽에서
 * `SignUpUiState.RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT` 식으로 참조.
 */
data class SignUpUiState(
    /** Step 1 입력값 — 이메일. */
    val email: String = "",
    /** Step 1 입력값 — 인증번호. */
    val verificationCode: String = "",
    /** 인증번호 발송 1회 이상 성공 여부. */
    val isVerificationSent: Boolean = false,
    /** 인증번호 전송 요청 진행 중. 버튼 중복 클릭 방지 + 로딩 텍스트 토글에 사용. */
    val isSendingCode: Boolean = false,
    /** 이메일/인증번호 검증 요청 진행 중. Step 1 "다음" 중복 클릭 방지. */
    val isVerifyingEmail: Boolean = false,
    /** 재전송 쿨다운 남은 초. 0 이면 즉시 재요청 가능. */
    val resendCooldownSeconds: Int = 0,
    /** 발송된 인증번호의 만료까지 남은 초. 0 이면 만료 (백엔드 Redis TTL 만료로 verify 도 거절). */
    val verificationRemainingSeconds: Int = 0,
    /** Step 2 입력값 — 주민등록번호 앞자리. */
    val residentFrontNumber: String = "",
    /** Step 2 입력값 — 주민등록번호 뒷자리 첫 1글자. */
    val residentBackNumber: String = "",
    /** Step 3 입력값 — 비밀번호. */
    val signUpPassword: String = "",
    /** Step 3 입력값 — 비밀번호 확인. */
    val signUpPasswordConfirm: String = "",
    /** Step 4 — 약관 동의 상태. */
    val termsState: TermsState = TermsState(),
    /** Profile — 사용자 이름. */
    val name: String = "",
    /** Profile — 프로필 이미지 Uri. 이미지 picker 가 선택된 Uri 를 push, 미선택 시 null. */
    val profileImageUri: Uri? = null,
    /** 회원가입 + 자동 로그인 진행 중. */
    val isLoading: Boolean = false,
    /** 회원가입 + 자동 로그인 성공. UI 가 홈으로 navigate 후 reset. */
    val isSignedUp: Boolean = false,
    /** Step 1 검증 통과 — 주민등록번호 단계로 이동. */
    val shouldNavigateToResidentNumber: Boolean = false,
    /** 이름 미입력 — UI 가 명시적 메시지 표시. */
    val isNameRequired: Boolean = false,
    /** snackbar 로 노출할 에러 메시지. */
    val errorMessage: String? = null,
) {
    val isEmailFormatValid: Boolean
        get() = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** Step 1 — 이메일·인증번호 입력 후 다음 단계 진행 가능 여부. */
    val isStep1NextEnabled: Boolean
        get() =
            !isVerifyingEmail &&
                isEmailFormatValid &&
                verificationCode.length >= MIN_VERIFICATION_CODE_LENGTH

    /** Step 2 — 주민등록번호 앞 6자리 + 뒷 첫 1자리. */
    val isStep2NextEnabled: Boolean
        get() =
            residentFrontNumber.length == RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT &&
                residentBackNumber.length == RESIDENT_REGISTRATION_BACK_FIRST_DIGIT_COUNT

    /** 비밀번호 정규식 충족 여부. 안내 문구 색상 토글에도 사용. */
    val isPasswordRuleSatisfied: Boolean
        get() = PASSWORD_REGEX.matches(signUpPassword)

    /** Step 3 — 비밀번호 규칙 충족 + 확인 일치. */
    val isStep3NextEnabled: Boolean
        get() = isPasswordRuleSatisfied && signUpPassword == signUpPasswordConfirm

    /** Step 4 — 필수 약관 (이용 · 개인정보) 동의. */
    val isStep4NextEnabled: Boolean
        get() = termsState.isTermsAgreed && termsState.isPrivacyAgreed

    companion object {
        /** 주민등록번호 앞자리 (생년월일) 자릿수. */
        const val RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT = 6

        /** 뒷자리 UI 에서 수집하는 첫 번째 마스킹 전 숫자 1자리. */
        const val RESIDENT_REGISTRATION_BACK_FIRST_DIGIT_COUNT = 1

        private const val MIN_VERIFICATION_CODE_LENGTH = 6

        /** 8~16자, 영문 대소문자 + 숫자 + 특수문자 각 1개 이상. */
        private val PASSWORD_REGEX =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,16}$")
    }
}
