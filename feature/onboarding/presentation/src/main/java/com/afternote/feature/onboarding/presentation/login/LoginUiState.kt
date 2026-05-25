package com.afternote.feature.onboarding.presentation.login

data class LoginUiState(
    val isLoading: Boolean = false,
    /** 로그인 성공 신호 — UI 가 LaunchedEffect 로 nav 후 [LoginViewModel.onLoggedInConsumed] 로 reset. */
    val isLoggedIn: Boolean = false,
    /** 서버/UseCase 가 내려준 사용자 친화 message. UI 가 snackbar 표시 후 [LoginViewModel.onErrorConsumed] 로 reset. */
    val errorMessage: String? = null,
)
