package com.afternote.feature.onboarding.presentation.login

/**
 * 로그인 화면 단일 UI 상태.
 *
 * 입력값(email/password) · 진행 플래그(isLoading) · 단발성 신호(isLoggedIn · shouldStartOnboarding · errorMessage)
 * 를 한 인스턴스로 묶어 reducer (`_uiState.update { copy(...) }`) 로 갱신한다.
 *
 * 단발성 신호는 UI 가 소비 후 [LoginViewModel.onLoggedInConsumed] / [LoginViewModel.onOnboardingStartConsumed] /
 * [LoginViewModel.onErrorConsumed] 호출로 reset.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** 기존 유저 로그인 성공 신호 — UI 가 LaunchedEffect 로 홈 nav 후 [LoginViewModel.onLoggedInConsumed] 로 reset. */
    val isLoggedIn: Boolean = false,
    /** 소셜 신규 가입자 신호 — UI 가 온보딩(Welcome) nav 후 [LoginViewModel.onOnboardingStartConsumed] 로 reset. */
    val shouldStartOnboarding: Boolean = false,
    /** 서버/UseCase 가 내려준 사용자 친화 message. UI 가 snackbar 표시 후 [LoginViewModel.onErrorConsumed] 로 reset. */
    val errorMessage: String? = null,
)
