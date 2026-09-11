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

    /**
     * 비밀번호 찾기 흐름의 진입 키 (#1789).
     *
     * 세 화면이 하나의 `FindPasswordViewModel` 을 공유해야 하는데, 온보딩 스택에 셋을 나란히
     * 두면 그 ViewModel 을 걸 자리가 host 밖에 없어 온보딩 전체와 수명이 같아진다. 흐름 키를
     * 한 칸 두고 그 안에서 다시 스택을 열면 «세 화면 사이 공유, 흐름 이탈 시 정리» 가 된다 —
     * 에디터·열람 신청 흐름과 같은 형태다.
     */
    @Serializable
    data object FindPasswordFlowRoute : OnboardingRoute

    /** 비밀번호 찾기 1단계 — 이메일 인증. */
    @Serializable
    data object FindPasswordRoute : OnboardingRoute

    /** 비밀번호 찾기 2단계 — 새 비밀번호 설정. */
    @Serializable
    data object FindPasswordResetRoute : OnboardingRoute

    /** 비밀번호 찾기 3단계 — 완료. */
    @Serializable
    data object FindPasswordCompleteRoute : OnboardingRoute
}
