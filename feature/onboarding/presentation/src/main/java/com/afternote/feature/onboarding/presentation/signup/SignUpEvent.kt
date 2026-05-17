package com.afternote.feature.onboarding.presentation.signup

/**
 * SignUp 플로우의 단발성 이벤트.
 *
 * ViewModel → UI 방향으로 한 번만 소비됩니다.
 */
sealed interface SignUpEvent {
    data object SignUpSuccess : SignUpEvent

    /** Step 1 이메일/인증번호 검증 통과 — 주민등록번호 단계로 이동 */
    data object NavigateToResidentNumber : SignUpEvent

    data class ShowError(
        val message: String,
    ) : SignUpEvent
}
