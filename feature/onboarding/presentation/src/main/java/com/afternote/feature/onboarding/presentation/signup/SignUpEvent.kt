package com.afternote.feature.onboarding.presentation.signup

/**
 * SignUp 플로우의 단발성 이벤트.
 *
 * ViewModel → UI 방향으로 한 번만 소비됩니다.
 */
sealed interface SignUpEvent {
    data object SignUpSuccess : SignUpEvent

    data class ShowError(
        val message: String,
    ) : SignUpEvent
}
