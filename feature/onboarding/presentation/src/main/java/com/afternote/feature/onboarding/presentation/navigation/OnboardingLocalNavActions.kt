package com.afternote.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.navigation.popOrExit
import com.afternote.core.ui.navigation.pushSingleTop
import com.afternote.core.ui.navigation.replaceAllWith

/**
 * 온보딩 화면 콜백을 로컬 백스택 조작으로 잇는다.
 *
 * 컴포저블이 아니라 평범한 클래스다 — 백스택 «모양» 은 컴포지션 없이 그대로 잴 수 있어야
 * 회귀 기준(#1601)을 JVM 테스트로 고정할 수 있기 때문이다.
 */
internal class OnboardingLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
    private val externalActions: OnboardingExternalActions,
) : OnboardingNavActions {
    override fun replaceOnboardingWithHome(): Unit = externalActions.replaceOnboardingWithHome()

    override fun navigateToReceivedRecords(): Unit = externalActions.navigateToReceivedRecords()

    /** 소셜 신규 가입자 — Login(과 그 아래)을 비우고 Welcome 하나로 수렴한다. */
    override fun replaceLoginWithWelcome(): Unit = backStack.replaceAllWith(OnboardingRoute.WelcomeRoute)

    /** Login 을 SignUp 으로 교체 — 뒤로가기는 Login 이 아니라 그 이전(Welcome)으로. */
    override fun replaceLoginWithSignUp() {
        backStack.remove(OnboardingRoute.LoginRoute)
        backStack.add(OnboardingRoute.SignUpRoute)
    }

    override fun navigateToSignUp(): Unit = backStack.pushSingleTop(OnboardingRoute.SignUpRoute)

    override fun navigateToLogin(): Unit = backStack.pushSingleTop(OnboardingRoute.LoginRoute)

    override fun navigateToFindId(): Unit = backStack.pushSingleTop(OnboardingRoute.FindIdRoute)

    override fun proceedToSignUpResidentNumber(): Unit = backStack.pushSingleTop(OnboardingRoute.SignUpResidentNumberRoute)

    override fun proceedToSignUpPassword(): Unit = backStack.pushSingleTop(OnboardingRoute.SignUpPasswordRoute)

    override fun proceedToTerms(): Unit = backStack.pushSingleTop(OnboardingRoute.TermsRoute)

    override fun proceedToProfile(): Unit = backStack.pushSingleTop(OnboardingRoute.ProfileRoute)

    override fun navigateToTermsDetail(): Unit = backStack.pushSingleTop(OnboardingRoute.TermsDetailRoute)

    override fun popBack(): Unit = backStack.popOrExit(boundary)
}
