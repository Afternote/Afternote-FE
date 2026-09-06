package com.afternote.feature.onboarding.presentation.navigation

/**
 * 온보딩 로컬 스택이 **스스로 갈 수 없는 곳**만 앱 셸에 남긴 이동.
 *
 * 그래프 안의 push/pop 은 [OnboardingNavHost] 가 로컬 백스택으로 직접 처리하고, 다른 소관
 * 그래프로 넘어가는 두 가지만 셸이 소유한다. 스택 바닥에서의 back 은 이동이 아니라 경계라
 * [com.afternote.core.ui.navigation.FeatureStackBoundary] 가 갖는다.
 */
public interface OnboardingExternalActions {
    /** 로그인·회원가입 성공 → 온보딩을 통째로 비우고 홈 진입. */
    public fun replaceOnboardingWithHome()

    /** Welcome 의 "전달 받은 기록 확인하기" → 수신자 흐름(받은 기록함) 진입. */
    public fun navigateToReceivedRecords()
}
