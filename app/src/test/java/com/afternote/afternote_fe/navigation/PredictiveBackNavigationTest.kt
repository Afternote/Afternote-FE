package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.afternote.core.ui.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Nav3 루트의 시스템 back 경계 회귀 기준 (#1702). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class PredictiveBackNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun start() {
        composeRule.setContent { SkeletonAppNavigation(Route.Home) { harness = it } }
        composeRule.waitForIdle()
    }

    @Test
    fun `홈 스택의 하위 화면에서 back 하면 해당 화면만 제거한다`() {
        start()
        composeRule.runOnIdle { harness.homeActions.onSettingClick() }
        assertEquals(listOf("NavHostRoot", "Home", "Setting"), composeRule.runOnIdle { harness.navController.backStackRouteNames() })
        assertTrue(composeRule.runOnIdle { harness.navController.popBackStack() })
        assertEquals(listOf("NavHostRoot", "Home"), composeRule.runOnIdle { harness.navController.backStackRouteNames() })
    }

    @Test
    fun `탭 루트에서 back 하면 홈 탭으로 돌아간다`() {
        start()
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }
        assertTrue(composeRule.runOnIdle { harness.navController.popBackStack() })
        assertEquals(listOf("NavHostRoot", "Home"), composeRule.runOnIdle { harness.navController.backStackRouteNames() })
    }

    @Test
    fun `홈 루트에서 back 하면 앱 종료 경계로 전달된다`() {
        start()
        assertFalse(composeRule.runOnIdle { harness.navController.popBackStack() })
    }
}
