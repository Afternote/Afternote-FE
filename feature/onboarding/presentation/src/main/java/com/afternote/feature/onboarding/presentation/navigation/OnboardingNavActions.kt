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

    /**
     * Welcome 의 "전달 받은 기록 확인하기" 누름 → 수신자 흐름 진입 (받은 기록함, 이슈 #215).
     * 본인 확인 캐시 상태와 무관하게 받은 기록함으로 직행하며, 본인 확인은 발신자별 열람 신청 시점에 1회 수행.
     */
    fun onNavigateWelcomeToReceivedRecords()

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

    fun onViewTerms()

    fun onTermsDetailBack()

    fun onProfileBack()
}
