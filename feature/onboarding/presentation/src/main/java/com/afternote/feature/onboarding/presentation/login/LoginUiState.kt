package com.afternote.feature.onboarding.presentation.login

/**
 * 로그인 화면 단일 UI 상태.
 *
 * 입력값(email/password) · 진행 플래그(isLoading) · 단발성 신호(loginSucceeded · errorMessage)
 * 를 한 인스턴스로 묶어 reducer (`_uiState.update { copy(...) }`) 로 갱신한다.
 *
 * 단발성 신호는 UI 가 소비 후 [LoginViewModel.onLoginSuccessConsumed] /
 * [LoginViewModel.onErrorMessageConsumed] 호출로 reset.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** 로그인 성공 신호. UI 가 navigate 후 [LoginViewModel.onLoginSuccessConsumed] 호출로 reset. */
    val loginSucceeded: Boolean = false,
    /** snackbar 로 노출할 에러 메시지. UI 가 표시 후 [LoginViewModel.onErrorMessageConsumed] 호출로 reset. */
    val errorMessage: String? = null,
)
