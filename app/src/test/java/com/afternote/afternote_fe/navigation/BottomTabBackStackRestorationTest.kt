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
 * 대상은 [AppState.navigateToBottomBarRoute] 의 `popUpTo<Route.Home> { saveState }` +
 * `restoreState` 조합이다. Navigation 3 전환은 이 조합을 통째로 다른 기전으로 바꾸므로,
 * **전환 전 현행 동작**을 여기에 못박아 둔다.
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
        composeRule.onNodeWithText("TimeLetterDraftRoute#0").performClick()
        composeRule.onNodeWithText("TimeLetterDraftRoute#1").assertExists()

        // 홈 탭으로 나가면 타임레터 스택은 통째로 pop 되고(=화면이 사라진다) 저장만 된다.
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Home) }
        assertEquals(
            listOf("NavHostRoot", "Home"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }

        // 탭 루트가 아니라 «떠날 때 보고 있던 화면» 으로 돌아온다.
        assertEquals(
            listOf("NavHostRoot", "Home", "TimeLetter", "TimeLetterHomeRoute", "TimeLetterDraftRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
        // 화면 안의 rememberSaveable 상태까지 살아 돌아온다 (saveState = true 의 실제 효과).
        composeRule.onNodeWithText("TimeLetterDraftRoute#1").assertExists()
    }

    @Test
    fun `탭 이동은 홈을 백스택 바닥에 남기고 탭 스택을 쌓지 않는다`() {
        start()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.MindRecord) }
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.MindRecord) }

        // 탭을 몇 번 오가도 백스택은 [홈, 현재 탭] 두 칸이다 — 탭이 서로 쌓이지 않는다.
        assertEquals(
            listOf("NavHostRoot", "Home", "MindRecord"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )

        // 뒤로가기 한 번이면 홈. 홈에서 한 번 더면 앱을 벗어난다(=pop 실패).
        assertEquals(true, composeRule.runOnIdle { harness.navController.popBackStack() })
        assertEquals("Home", composeRule.runOnIdle { harness.navController.currentRouteName() })
        assertEquals(false, composeRule.runOnIdle { harness.navController.popBackStack() })
    }

    @Test
    fun `애프터노트 탭만은 재진입 때 저장된 스택 대신 지문 관문으로 돌아간다`() {
        start()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Afternote) }
        assertEquals(
            "FingerprintLoginRoute",
            composeRule.runOnIdle { harness.navController.currentRouteName() },
        )

        composeRule.runOnIdle { harness.afternoteActions.replaceFingerprintLoginWithAfternoteHome() }
        composeRule.runOnIdle { harness.afternoteActions.navigateToAfternoteDetail(itemId = 7L) }
        assertEquals(
            "DetailRoute",
            composeRule.runOnIdle { harness.navController.currentRouteName() },
        )

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Home) }
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Afternote) }

        // restoreState = false 라 상세·홈이 복원되지 않고 인증 관문이 다시 선다.
        assertEquals(
            listOf("NavHostRoot", "Home", "Afternote", "FingerprintLoginRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
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
        composeRule.onNodeWithText("TimeLetterDraftRoute#0").performClick()
        val beforeRestore = composeRule.runOnIdle { harness.navController }

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        // 재생성이 실제로 일어났는지부터 확인한다 — 같은 NavController 를 계속 쓰고 있다면
        // 아래 단언은 아무것도 증명하지 못한다.
        assertNotSame(beforeRestore, composeRule.runOnIdle { harness.navController })
        assertEquals(
            listOf("NavHostRoot", "Home", "TimeLetter", "TimeLetterHomeRoute", "TimeLetterDraftRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
        composeRule.onNodeWithText("TimeLetterDraftRoute#1").assertExists()
    }
}
