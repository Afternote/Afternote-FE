package com.afternote.feature.onboarding.presentation.navigation

/**
 * 온보딩 그래프로 전달되는 네비게이션 이벤트 묶음.
 *
 * [onboardingNavGraph]의 파라미터 비대화를 줄이고, 앱 루트에서 `remember`로 안정적으로 묶기 위함이다.
 */
interface OnboardingNavActions {
    fun onOnboardingComplete()

    fun onNavigateWelcomeToSignUp()

    fun onNavigateWelcomeToLogin()

    fun onReplaceLoginWithSignUp()

    fun onLoginBack()

    fun onSignUpEmailNext()

    fun onSignUpEmailBack()

    fun onSignUpResidentNext()

    fun onSignUpResidentBack()

    fun onSignUpPasswordNext()

    fun onSignUpPasswordBack()

    fun onTermsNext()

    fun onTermsBack()

    fun onProfileBack()
}
