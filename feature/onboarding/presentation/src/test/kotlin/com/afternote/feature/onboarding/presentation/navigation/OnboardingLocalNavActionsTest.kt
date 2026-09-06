package com.afternote.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 온보딩 로컬 백스택 경계 회귀 기준 — Nav2 시절 `AuthBoundaryBackStackTest` 가 보던 몫 (#1601 · #1698).
 *
 * 이관 전에는 `popUpTo` 옵션 조합의 «결과» 를 루트 NavController 에서 읽었다. 로컬 스택에선
 * 결과 상태를 직접 만들므로, 그 결과를 스택 모양으로 그대로 못박는다. 컴포지션이 필요 없어
 * Robolectric 없이 돈다.
 */
class OnboardingLocalNavActionsTest {
    private val exits = mutableListOf<Unit>()
    private val backStack = NavBackStack<NavKey>(OnboardingRoute.WelcomeRoute)
    private val external = RecordingExternalActions()
    private val actions =
        OnboardingLocalNavActions(
            backStack = backStack,
            boundary = FeatureStackBoundary { exits += Unit },
            externalActions = external,
        )

    private fun stack(): List<String> = backStack.map { it::class.simpleName!! }

    @Test
    fun `로그인 화면 교체 이동은 로그인을 백스택에 남기지 않는다`() {
        actions.navigateToLogin()

        actions.replaceLoginWithSignUp()

        assertEquals(listOf("WelcomeRoute", "SignUpRoute"), stack())
        // 가입 화면에서 뒤로가기는 로그인이 아니라 Welcome 으로 간다.
        actions.popBack()
        assertEquals(listOf("WelcomeRoute"), stack())
    }

    @Test
    fun `소셜 신규 가입 분기는 Welcome 을 새로 세우고 로그인을 남기지 않는다`() {
        actions.navigateToLogin()
        actions.navigateToFindId()

        actions.replaceLoginWithWelcome()

        assertEquals(listOf("WelcomeRoute"), stack())
    }

    @Test
    fun `회원가입 4단계는 순서대로 쌓이고 뒤로가기로 한 칸씩 내려온다`() {
        actions.navigateToLogin()
        actions.replaceLoginWithSignUp()
        actions.proceedToSignUpResidentNumber()
        actions.proceedToSignUpPassword()
        actions.proceedToTerms()
        actions.proceedToProfile()

        assertEquals(
            listOf(
                "WelcomeRoute",
                "SignUpRoute",
                "SignUpResidentNumberRoute",
                "SignUpPasswordRoute",
                "TermsRoute",
                "ProfileRoute",
            ),
            stack(),
        )

        actions.popBack()
        assertEquals("TermsRoute", stack().last())
    }

    @Test
    fun `같은 화면을 연달아 요청해도 두 번 쌓이지 않는다`() {
        actions.navigateToLogin()
        actions.navigateToLogin()

        assertEquals(listOf("WelcomeRoute", "LoginRoute"), stack())
    }

    @Test
    fun `스택 바닥에서의 뒤로가기는 스택을 비우지 않고 셸에 넘긴다`() {
        actions.popBack()

        assertEquals(listOf("WelcomeRoute"), stack())
        assertEquals(1, exits.size)
    }

    @Test
    fun `그래프 밖 이동은 스택을 건드리지 않고 셸로 나간다`() {
        actions.navigateToLogin()

        actions.replaceOnboardingWithHome()
        actions.navigateToReceivedRecords()

        assertTrue(external.replacedWithHome)
        assertTrue(external.wentToReceivedRecords)
        assertEquals(listOf("WelcomeRoute", "LoginRoute"), stack())
    }

    private class RecordingExternalActions : OnboardingExternalActions {
        var replacedWithHome = false
        var wentToReceivedRecords = false

        override fun replaceOnboardingWithHome() {
            replacedWithHome = true
        }

        override fun navigateToReceivedRecords() {
            wentToReceivedRecords = true
        }
    }
}
