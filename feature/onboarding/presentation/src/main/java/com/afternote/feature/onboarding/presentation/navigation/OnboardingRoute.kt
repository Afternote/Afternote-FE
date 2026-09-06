package com.afternote.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 온보딩 로컬 Navigation 3 스택의 키.
 *
 * [NavKey] 는 Nav3 백스택에 실릴 수 있다는 표식이고, `@Serializable` 은 프로세스 재생성 뒤
 * 스택을 복원하는 데 쓰인다 — 둘 다 있어야 `rememberNavBackStack` 이 이 키를 저장한다.
 */
sealed interface OnboardingRoute : NavKey {
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
}
