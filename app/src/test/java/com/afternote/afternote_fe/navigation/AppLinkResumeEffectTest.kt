package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.compose.NavHost
import com.afternote.core.common.deeplink.NavigationTarget
import com.afternote.core.ui.Route
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 링크 재개의 **시점**과 **횟수** 회귀 기준 (#924).
 *
 * 어디로 가는지는 `AppLinkTargetBackStackTest` 가, 어떤 값이 재개 가능해지는지는
 * `MainViewModelAppLinkTest` 가 본다. 여기 남는 것은 그 둘 사이 — 「지금 쌓아도 되는가」다.
 *
 * 로그인 성공 직후가 그 판정이 필요한 구간이다. 세션이 저장돼 관문은 열렸지만 온보딩 스택이
 * 아직 루트를 비우기 전이라, 그 사이에 쌓은 화면은 뒤따르는 `popUpTo(0)` 에 함께 지워진다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class AppLinkResumeEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var appState: AppState
    private val resumed = mutableListOf<NavigationTarget>()
    private var target by mutableStateOf<NavigationTarget?>(null)

    /** 프로덕션과 같은 순서 — 셸을 먼저 세우고 그 위에 재개 adapter 를 건다. */
    private fun start(startDestination: Route) {
        composeRule.setContent {
            val state = rememberAfternoteAppState()
            SideEffect { appState = state }

            NavHost(
                navController = state.navController,
                startDestination = startDestination,
            ) {
                appRouteSkeleton()
            }

            AppLinkResumeEffect(
                appState = state,
                resumableTarget = target,
                onTargetResumed = resumed::add,
            )
        }
        composeRule.waitForIdle()
    }

    private fun routes(): List<String> = composeRule.runOnIdle { appState.navController.backStackRouteNames() }

    private fun arrive(target: NavigationTarget) {
        composeRule.runOnIdle { this.target = target }
        composeRule.waitForIdle()
    }

    @Test
    fun `온보딩을 지나기 전에는 재개하지 않는다`() {
        start(Route.Onboarding)

        arrive(TARGET)

        assertEquals(emptyList<NavigationTarget>(), resumed)
        assertEquals(listOf("NavHostRoot", "Onboarding"), routes())
    }

    @Test
    fun `온보딩을 떠난 뒤에 기다리던 목적지를 연다`() {
        start(Route.Onboarding)
        arrive(TARGET)

        composeRule.runOnIdle { replaceOnboardingWithHome() }
        composeRule.waitForIdle()

        assertEquals(listOf(TARGET), resumed)
        assertEquals(listOf("NavHostRoot", "Home", "DailyQuestionWriteRoute"), routes())
    }

    /** 소비 신호가 늦게 와도 그 사이의 리컴포지션·이동이 같은 목적지를 다시 열지 않는다. */
    @Test
    fun `목적지 하나는 한 번만 재개된다`() {
        start(Route.Home)
        arrive(TARGET)

        composeRule.runOnIdle { appState.navigateToBottomBarRoute(Route.Home) }
        composeRule.waitForIdle()

        assertEquals(listOf(TARGET), resumed)
    }

    /** 로그인 성공이 하는 일과 같은 이동 — `OnboardingExternalActions.replaceOnboardingWithHome`. */
    private fun replaceOnboardingWithHome() {
        appState.navController.navigate(Route.Home) {
            popUpTo(0) { inclusive = true }
        }
    }

    private companion object {
        val TARGET: NavigationTarget = NavigationTarget.DailyQuestionCompose
    }
}
