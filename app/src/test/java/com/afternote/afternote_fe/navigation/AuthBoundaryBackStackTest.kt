package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.afternote.core.ui.Route
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 인증 전후 **루트** 백스택 경계 회귀 기준 (#1601).
 *
 * 이 앱의 인증 게이트는 세 겹이다 — 로그인(`MainViewModel.startRoute`) · 지문(애프터노트 로컬
 * 스택의 시작 화면) · 수신자 본인 확인(열람 신청 흐름). Navigation 3 이관(#1698) 뒤 뒤의 두
 * 게이트는 피처 로컬 스택 안에 있으므로, 그 경계는 각 피처의 `*LocalNavActions` 테스트가 본다:
 *
 * - 지문 관문 → `AfternoteLocalNavActionsTest`
 * - 열람 신청 단계 소거 → `DeliveryVerificationFlowLocalNavActionsTest`
 * - 로그인 화면 교체·소셜 신규 가입 분기 → `OnboardingLocalNavActionsTest`
 *
 * 여기 남는 것은 **루트가 소유한 경계**뿐이다 — 온보딩↔홈 전환과 로그아웃·탈퇴.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class AuthBoundaryBackStackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun start(startDestination: Route) {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = startDestination) { harness = it }
        }
        composeRule.waitForIdle()
    }

    private fun routes(): List<String> = composeRule.runOnIdle { harness.navController.backStackRouteNames() }

    @Test
    fun `로그인 성공은 온보딩을 루트에서 비우고 홈만 남긴다`() {
        start(Route.Onboarding)

        composeRule.runOnIdle { harness.onboardingExternalActions.replaceOnboardingWithHome() }

        // popUpTo(0) { inclusive } — 온보딩 host destination 까지 사라진다.
        assertEquals(listOf("NavHostRoot", "Home"), routes())
        // 홈에서 뒤로가기는 앱을 벗어난다(=pop 실패). 온보딩으로 되돌아가지 않는다.
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `온보딩 Welcome 의 받은 기록 확인은 수신자 흐름을 새로 쌓는다`() {
        start(Route.Onboarding)

        composeRule.runOnIdle { harness.onboardingExternalActions.navigateToReceivedRecords() }

        assertEquals(listOf("NavHostRoot", "Onboarding", "Receiver"), routes())
    }

    @Test
    fun `로그아웃은 인증 이후 스택을 전부 비우고 온보딩만 남긴다`() {
        start(Route.Home)

        composeRule.runOnIdle { harness.homeActions.onSettingClick() }
        composeRule.runOnIdle { harness.settingActions.onNavigateToWithdrawGuide() }

        composeRule.runOnIdle { harness.settingActions.onLogoutSuccess() }

        assertEquals(listOf("NavHostRoot", "Onboarding"), routes())
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `탈퇴는 로그아웃과 같은 경계로 스택을 비운다`() {
        start(Route.Home)

        composeRule.runOnIdle { harness.homeActions.onSettingClick() }
        composeRule.runOnIdle { harness.settingActions.onNavigateToWithdrawGuide() }
        composeRule.runOnIdle { harness.settingActions.onNavigateToWithdrawConfirm() }

        composeRule.runOnIdle { harness.settingActions.onWithdrawSuccess() }

        assertEquals(listOf("NavHostRoot", "Onboarding"), routes())
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `이관된 그래프는 루트에 host destination 한 칸만 쌓는다`() {
        start(Route.Home)

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Afternote) }

        // Nav2 시절엔 [Afternote, FingerprintLoginRoute] 두 칸이었다. 이관 뒤 그래프 안쪽 화면은
        // 로컬 스택에 있으므로 루트는 host 한 칸만 본다 — #1702 가 루트를 바꿀 때 이 모양을 받는다.
        assertEquals(listOf("NavHostRoot", "Home", "Afternote"), routes())

        // 로컬 스택 바닥에서의 back 은 boundary 로 올라와 이 한 칸을 pop 한다.
        assertEquals(true, composeRule.runOnIdle { harness.navController.popBackStack() })
        assertEquals("Home", composeRule.runOnIdle { harness.navController.currentRouteName() })
    }
}
