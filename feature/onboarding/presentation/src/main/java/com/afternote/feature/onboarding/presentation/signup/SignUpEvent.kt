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

    /** 이름 미입력 같은 클라이언트 측 검증 실패 — UI 가 명시적 메시지 매핑. */
    data object NameRequired : SignUpEvent

    /** 백엔드 등 외부 호출 실패. [message] 가 null 이면 UI 가 일반 fallback 메시지 사용. */
    data class ShowError(
        val message: String?,
    ) : SignUpEvent
}
