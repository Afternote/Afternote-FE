package com.afternote.feature.onboarding.presentation.signup

import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.onboarding.presentation.OnboardingEmailRule
import com.afternote.feature.onboarding.presentation.OnboardingFailure
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
internal data class SignUpUiState(
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
     * 회원가입 POST 가 성공한 **자격**. 자동 로그인만 실패해 같은 화면에 남았을 때, 재제출이
     * 가입을 다시 호출하지 않도록 가른다 — 다시 부르면 서버가 이메일 중복으로 거절해 복구
     * 자체가 막힌다 (#710).
     *
     * Boolean 이 아니라 자격을 드는 이유는, 부분 성공 뒤 뒤로 가 이메일·비밀번호를 고치면
     * **그 플래그가 새 입력에도 그대로 남기 때문이다** (#2026). 그러면 새 계정의 가입은
     * 건너뛰고 새 자격으로 로그인만 불러, 만들어진 적 없는 계정으로 복구가 돈다.
     */
    val createdAccount: CreatedSignUpAccount? = null,
    /** 회원가입 + 자동 로그인 성공. UI 가 홈으로 navigate 후 reset. */
    val isSignedUp: Boolean = false,
    /** Step 1 검증 통과 — 주민등록번호 단계로 이동. */
    val shouldNavigateToResidentNumber: Boolean = false,
    /** 이름 미입력 — UI 가 명시적 메시지 표시. */
    val isNameRequired: Boolean = false,
    /** 실패 한 건의 사유. 화면이 인라인 또는 스낵바로 표시한다. */
    val failure: OnboardingFailure? = null,
) : UiState {
    /** 지금 입력이 [createdAccount] 와 같은 자격인지 — 같을 때만 가입을 건너뛴다 (#2026). */
    internal val isAccountCreated: Boolean
        get() = createdAccount == CreatedSignUpAccount(email = email, password = signUpPassword)

    /**
     * 인증 결과가 **지금 화면의 입력에 대한 답인지** (#2025).
     *
     * 이메일 칸은 인증이 도는 동안에도 고칠 수 있다. 보낸 입력과 지금 입력이 다르면 그 결과는
     * 이미 사용자가 버린 시도의 것이고, 적용하면 서버가 검증한 적 없는 이메일로 다음 단계가 열린다.
     */
    internal fun matches(
        email: String,
        certificateCode: String,
    ): Boolean = this.email == email && this.verificationCode == certificateCode

    /** 이메일 형식 검사 — 세 온보딩 화면이 [OnboardingEmailRule] 하나를 쓴다 (#1851). */
    val isEmailFormatValid: Boolean
        get() = OnboardingEmailRule.isValid(email)

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

/** 회원가입 POST 가 성공한 자격 한 벌. 재제출이 같은 계정을 가리키는지 판정하는 단위다 (#2026). */
internal data class CreatedSignUpAccount(
    val email: String,
    val password: String,
)
