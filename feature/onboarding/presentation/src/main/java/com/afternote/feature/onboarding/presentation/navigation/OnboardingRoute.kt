package com.afternote.feature.onboarding.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface OnboardingRoute {
    @Serializable
    data object WelcomeRoute : OnboardingRoute

    @Serializable
    data object LoginRoute : OnboardingRoute

    @Serializable
    data object SignUpRoute : OnboardingRoute

    @Serializable
    data object SignUpResidentNumberRoute : OnboardingRoute

    @Serializable
    data object SignUpPasswordRoute : OnboardingRoute

    @Serializable
    data object TermsRoute : OnboardingRoute

    @Serializable
    data object TermsDetailRoute : OnboardingRoute

    @Serializable
    data object ProfileRoute : OnboardingRoute

    @Serializable
    data object FindIdRoute : OnboardingRoute

    /** 비밀번호 찾기 1단계 — 이메일 인증. */
    @Serializable
    data object FindPasswordRoute : OnboardingRoute

    /** 비밀번호 찾기 2단계 — 새 비밀번호 입력. 제출이 곧 인증번호 검증이다. */
    @Serializable
    data object FindPasswordResetRoute : OnboardingRoute

    /** 비밀번호 찾기 3단계 — 변경 완료 안내. */
    @Serializable
    data object FindPasswordCompleteRoute : OnboardingRoute
}
