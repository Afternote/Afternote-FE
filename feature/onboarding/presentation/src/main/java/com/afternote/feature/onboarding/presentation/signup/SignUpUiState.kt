package com.afternote.feature.onboarding.presentation.signup

import android.util.Patterns
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.OnboardingPasswordRule
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
 * 폼 상수는 본 data class 의 companion 으로 묶어 ViewModel + UI 양쪽에서
 * `SignUpUiState.RESIDENT_REGISTRATION_FRONT_DIGIT_COUNT` 식으로 참조한다. 새 비밀번호 규칙은
 * 다른 온보딩 흐름에서도 재사용할 수 있도록 [OnboardingPasswordRule]에 둔다.
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
    /**
     * Profile — 프로필 이미지 Uri 의 String 표현. UI 가 picker `Uri.toString()` 으로 push,
     * 표시 시 `toUri()` 로 다시 변환. VM 이 framework `android.net.Uri` 의존을 갖지 않도록 String 보관.
     */
    val profileImageUri: String? = null,
    /** 회원가입 + 자동 로그인 진행 중. */
    val isLoading: Boolean = false,
    /**
     * 회원가입 POST 가 이미 성공한 상태. 자동 로그인만 실패해 같은 화면에 남았을 때,
     * 재제출이 가입을 다시 호출하지 않도록 가른다 — 다시 부르면 서버가 이메일 중복으로
     * 거절해 복구 자체가 막힌다.
     */
    val isAccountCreated: Boolean = false,
    /** 회원가입 + 자동 로그인 성공. UI 가 홈으로 navigate 후 reset. */
    val isSignedUp: Boolean = false,
    /** Step 1 검증 통과 — 주민등록번호 단계로 이동. */
    val shouldNavigateToResidentNumber: Boolean = false,
    /** 이름 미입력 — UI 가 명시적 메시지 표시. */
    val isNameRequired: Boolean = false,
    /**
     * 인증번호 무효(서버 code 1207) — 시안(2431:14204)상 인증번호 필드 아래 인라인 문구로
     * 표시(스낵바 아님). 만료 판정은 서버가 한다.
     *
     * 표시 문구는 화면의 고정 리소스라 서버 문구를 담지 않고 발생 여부만 든다.
     */
    val hasVerificationError: Boolean = false,
    /**
     * 인증번호 무효 외 실패(네트워크 등) — snackbar 로 노출할 문구.
     *
     * 예외 `message` 원문을 담지 않는다 — 사유를 확인해 준 타입만 각자의 문구를 갖고 나머지는
     * 정적 리소스로 내려앉는다(`Throwable.toDisplayMessage`).
     */
    val errorMessage: UiText? = null,
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
        get() = OnboardingPasswordRule.isSatisfied(signUpPassword)

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
    }
}
