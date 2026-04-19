package com.afternote.feature.onboarding.presentation.login

/**
 * Login 플로우의 단발성 이벤트.
 *
 * ViewModel → UI 방향으로 한 번만 소비됩니다.
 */
sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent

    data class ShowError(
        val message: String,
    ) : LoginEvent
}
