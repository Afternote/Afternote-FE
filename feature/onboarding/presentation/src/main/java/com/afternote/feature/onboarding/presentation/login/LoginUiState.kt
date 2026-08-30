package com.afternote.feature.onboarding.presentation.login

import com.afternote.core.ui.UiText

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
    /**
     * 자격 거절 인라인(시안 `3628:23437`). 소비형 신호가 아니라 상태다 — 입력이 바뀌면 그 자격의
     * 판정이 아니게 되므로 update 콜백이 해제한다.
     */
    val hasCredentialError: Boolean = false,
    /** 전송 계층 실패의 재시도 팝업(시안 `3628:23575`). retry·dismiss 로 해제. */
    val showNetworkErrorPopup: Boolean = false,
    /**
     * 위 두 갈래 밖의 실패(소셜 거절·미분류)는 스낵바 — 인라인을 걸 필드도, 재시도로 풀릴 보장도
     * 없다. 문구는 리소스 고정이라 서버 5xx 본문·역직렬화 원문이 화면에 닿지 않는다.
     * UI 가 표시 후 [LoginViewModel.onErrorConsumed] 로 reset.
     */
    val errorMessage: UiText? = null,
    /**
     * 패스키 인증 요청 원문 — 서버에서 옵션을 받아 오면 채워지는 **소비형 신호**다.
     *
     * 값 자체를 화면이 그리지는 않는다. Credential Manager 호출은 Activity 를 쥐고 있어야 해서
     * UI 계층에만 둘 수 있고(`LoginEntry`), ViewModel 은 "이제 시스템 선택기를 띄울 수 있다" 는
     * 사실만 여기 실어 보낸다. UI 가 집어간 뒤 [LoginViewModel.onPasskeyRequestConsumed] 로 reset.
     */
    val passkeyRequestJson: String? = null,
)
