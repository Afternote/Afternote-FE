package com.afternote.feature.onboarding.presentation.findaccount

import android.util.Patterns
import com.afternote.core.ui.UiText
import com.afternote.feature.onboarding.presentation.OnboardingPasswordRule

/**
 * 비밀번호 찾기 흐름(이메일 인증 → 비밀번호 변경 → 완료)의 단일 상태.
 *
 * 아이디 찾기([FindIdUiState])와 두 곳이 다르다.
 *
 * 1. **인증번호를 미리 확인하지 않는다.** 서버가 인증번호를 검증하면서 삭제하므로
 *    (BE `EmailService.verifyAndDeleteCode`) 여기서 한 번 쓰면 최종 제출에 쓸 코드가 없다.
 *    시안(`2431:14299`)에 인증번호 필드의 인라인 "확인" 이 없는 것도 같은 이유다 —
 *    검증은 `auth/password/find` 가 새 비밀번호와 함께 한 번에 한다.
 * 2. **소셜 가입 계정을 인증번호 발송 단계에서 거른다.** BE 가 `auth/find/send/code` 에서
 *    `password == null` 인 계정을 code 1702 로 거절하므로(`findActiveLocalUserForRecovery`),
 *    코드 입력 전에 차단 팝업(시안 `2383:16667`)을 낼 수 있다.
 *
 * @property resendCooldownSeconds "재전송" 잠금의 남은 초 — 발송 성공마다 30초로 재잠금되는
 *   클라이언트 측 연타 방지이며 **인증번호 유효시간과 무관**하다(만료 판정은 서버 몫).
 * @property isSocialSignUpAccount 소셜 가입 계정(서버 code 1702)이라 이 흐름을 쓸 수 없다는 사실.
 *   시안상 스낵바가 아니라 차단 팝업으로 표시한다.
 * @property isPasswordChanged 재설정 성공 — 완료 화면으로 넘기는 단발성 신호. 소비 후
 *   [FindPasswordViewModel.onPasswordResetConsumed] 로 흐름 상태 전체가 초기화된다.
 * @property errorMessage 팝업으로 가르지 않는 실패 — 스낵바로 표시.
 */
data class FindPasswordUiState(
    val email: String = "",
    val certificateCode: String = "",
    val isSendingCode: Boolean = false,
    val isVerificationSent: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val isSocialSignUpAccount: Boolean = false,
    val newPassword: String = "",
    val newPasswordConfirm: String = "",
    val isSubmitting: Boolean = false,
    val isPasswordChanged: Boolean = false,
    val errorMessage: UiText? = null,
) {
    /** 이메일 형식 검사. 회원가입·아이디 찾기와 동일 방식([Patterns.EMAIL_ADDRESS] 전체 일치). */
    val isEmailFormatValid: Boolean
        get() = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** 인증번호 발송/재발송 가능 여부. 쿨다운 중이거나 이메일 형식이 틀리면 막는다. */
    val isSendCodeEnabled: Boolean
        get() = !isSendingCode && resendCooldownSeconds == 0 && isEmailFormatValid

    /**
     * 이메일 인증 화면의 "다음" 활성 여부.
     *
     * 인증번호를 서버에 물어보지 않은 채 다음 화면으로 넘긴다 — 자릿수만 본다. 코드가 틀렸는지는
     * 최종 제출이 알려주고, 그 실패는 스낵바(`onboarding_find_password_code_expired`)로 돌아온다.
     */
    val isVerificationNextEnabled: Boolean
        get() = isVerificationSent && isEmailFormatValid && certificateCode.length >= MIN_VERIFICATION_CODE_LENGTH

    /** 비밀번호 규칙 충족 여부. 안내 문구 색상 토글에도 쓴다 — 회원가입과 같은 규칙. */
    val isPasswordRuleSatisfied: Boolean
        get() = OnboardingPasswordRule.isSatisfied(newPassword)

    /** 비밀번호 변경 화면의 "다음" 활성 여부 — 규칙 충족 + 확인 일치. 제출 중이면 연타를 막는다. */
    val isResetEnabled: Boolean
        get() = !isSubmitting && isPasswordRuleSatisfied && newPassword == newPasswordConfirm

    companion object {
        /** 서버가 요구하는 인증번호 자릿수 (BE `PasswordFindRequest` — `^[0-9]{6}$`). */
        const val MIN_VERIFICATION_CODE_LENGTH = 6
    }
}
