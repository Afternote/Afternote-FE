package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.onboarding.presentation.OnboardingFailure

/**
 * 로그인 화면 단일 UI 상태.
 *
 * 입력값(email/password) · 진행 플래그(isLoading) · 단발성 신호(isLoggedIn · shouldStartOnboarding · failure)
 * 를 한 인스턴스로 묶어 reducer (`_uiState.update { copy(...) }`) 로 갱신한다.
 *
 * 단발성 신호는 UI 가 소비 후 [LoginViewModel.onLoggedInConsumed] / [LoginViewModel.onOnboardingStartConsumed] /
 * [LoginViewModel.onErrorConsumed] 호출로 reset.
 */
internal data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** 기존 유저 로그인 성공 신호 — UI 가 LaunchedEffect 로 홈 nav 후 [LoginViewModel.onLoggedInConsumed] 로 reset. */
    val isLoggedIn: Boolean = false,
    /** 소셜 신규 가입자 신호 — UI 가 온보딩(Welcome) nav 후 [LoginViewModel.onOnboardingStartConsumed] 로 reset. */
    val shouldStartOnboarding: Boolean = false,
    /** 실패 한 건의 사유. 입력 수정·재시도·화면 소비로 해제한다. */
    val failure: OnboardingFailure? = null,
) : UiState
