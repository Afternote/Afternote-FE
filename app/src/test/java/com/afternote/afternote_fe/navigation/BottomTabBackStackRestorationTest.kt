package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 바텀 탭 전환의 백스택 저장/복원 회귀 기준 (#1601).
 *
 * 각 탭이 독립된 Navigation 3 백스택을 유지하고, 전환 후 해당 탭 화면이 복원되는지 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class BottomTabBackStackRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun start(startDestination: Route = Route.Home) {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = startDestination) { harness = it }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `탭을 떠났다 돌아오면 그 탭의 백스택과 화면 상태가 함께 복원된다`() {
        start()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }
        composeRule.runOnIdle { harness.timeLetterActions.onNavigateToDraft() }

        // 홈 탭으로 전환해도 타임레터의 로컬 Nav2 백스택은 유지된다.
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Home) }
        assertEquals(
            listOf("NavHostRoot", "Home"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }

        // 탭을 다시 열면 떠날 때 보고 있던 화면으로 돌아온다.
        assertEquals(
            listOf("NavHostRoot", "TimeLetter", "TimeLetterHomeRoute", "TimeLetterDraftRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
    }

    @Test
    fun `탭 이동은 홈을 백스택 바닥에 남기고 탭 스택을 쌓지 않는다`() {
        start()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.MindRecord) }
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.MindRecord) }

        // 탭 전환은 서로 다른 루트 스택을 활성화한다.
        assertEquals(
            listOf("NavHostRoot", "MindRecord"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )

        // 뒤로가기 한 번이면 홈. 홈에서 한 번 더면 앱을 벗어난다(=pop 실패).
        assertEquals(true, composeRule.runOnIdle { harness.navController.popBackStack() })
        assertEquals("Home", composeRule.runOnIdle { harness.navController.currentRouteName() })
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `애프터노트 탭만은 재진입 때 저장된 화면 상태를 복원하지 않는다`() {
        start()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Afternote) }
        composeRule.onNodeWithText("Afternote#0").performClick()
        composeRule.onNodeWithText("Afternote#1").assertExists()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Home) }
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Afternote) }

        // restoreState = false — 저장된 화면 상태가 복원되지 않고 host 가 새로 선다. 그래서
        // 로컬 스택도 시작 화면(지문 관문)부터 다시 쌓인다. 그 시작점 자체는
        // AfternoteLocalNavActionsTest 가 본다.
        assertEquals(
            listOf("NavHostRoot", "Afternote"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
        composeRule.onNodeWithText("Afternote#0").assertExists()
    }

    @Test
    fun `프로세스 재생성 후에도 현재 화면과 그 아래 백스택이 복원된다`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            SkeletonAppNavigation(startDestination = Route.Home) { harness = it }
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }
        composeRule.runOnIdle { harness.timeLetterActions.onNavigateToDraft() }
        val beforeRestore = composeRule.runOnIdle { harness.timeLetterRoutes }

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        // 재생성이 실제로 일어났는지부터 확인한다 — 같은 NavController 를 계속 쓰고 있다면
        // 아래 단언은 아무것도 증명하지 못한다.
        assertNotSame(beforeRestore, composeRule.runOnIdle { harness.timeLetterRoutes })
        assertEquals(
            listOf("NavHostRoot", "TimeLetter", "TimeLetterHomeRoute", "TimeLetterDraftRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
    }
}
