package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.activity.BackEventCompat
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.afternote.core.ui.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * predictive back 진행·취소·완료 회귀 기준 (#1601).
 *
 * 이 앱은 `enableOnBackInvokedCallback` 을 매니페스트에 적지 않지만 `targetSdk 36` 이라
 * 시스템 기본값으로 predictive back 이 켜지고, 화면 사이 처리는 `NavHost` 안의
 * `PredictiveBackHandler` 가 맡는다. 즉 **앱 코드가 명시적으로 쓴 적 없는 경로**가 백스택을
 * 건드린다 — Navigation 3 전환이 조용히 깨뜨리기 딱 좋은 자리다.
 *
 * 손가락 제스처 자체는 재현할 수 없지만 `OnBackPressedDispatcher` 의 진행 이벤트는 그대로 흘려
 * 넣을 수 있어, «중간에 취소하면 아무 일도 없고 끝까지 가면 한 칸만 pop 된다» 를 자동으로 잰다.
 * 진행 중 화면이 어떻게 **보이는지**(스케일·페이드·모서리 곡률)는 여기서 못 잰다 —
 * 그 몫은 `docs/qa/predictive-back.md` 의 수동 절차가 받는다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class PredictiveBackNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun startOnTimeLetterDraft() {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = Route.Home) { harness = it }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.TimeLetter) }
        composeRule.runOnIdle { harness.timeLetterActions.onNavigateToDraft() }
        assertEquals(DRAFT_STACK, routes())
    }

    private fun routes(): List<String> = composeRule.runOnIdle { harness.navController.backStackRouteNames() }

    private fun visibleRoutes(): List<String> =
        composeRule.runOnIdle {
            harness.navController.visibleEntries.value
                .map { it.destination.simpleRouteName() }
        }

    /** 손가락을 대고 40% 까지 끌었지만 아직 떼지 않은 상태를 만든다. */
    private fun beginGesture() {
        composeRule.runOnIdle {
            harness.backDispatcher.dispatchOnBackStarted(backEvent(progress = 0f))
        }
        composeRule.runOnIdle {
            harness.backDispatcher.dispatchOnBackProgressed(backEvent(progress = 0.4f))
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `진행 중인 predictive back 은 아직 백스택을 바꾸지 않는다`() {
        startOnTimeLetterDraft()
        assertEquals(listOf("TimeLetterDraftRoute"), visibleRoutes())

        beginGesture()

        assertEquals(DRAFT_STACK, routes())
        assertEquals(
            "TimeLetterDraftRoute",
            composeRule.runOnIdle { harness.navController.currentRouteName() },
        )
    }

    @Test
    fun `predictive back 을 취소하면 떠나려던 화면에 그대로 남는다`() {
        startOnTimeLetterDraft()

        beginGesture()
        composeRule.runOnIdle { harness.backDispatcher.dispatchOnBackCancelled() }
        composeRule.waitForIdle()

        assertEquals(DRAFT_STACK, routes())
        assertEquals(
            "TimeLetterDraftRoute",
            composeRule.runOnIdle { harness.navController.currentRouteName() },
        )
        // 제스처가 NavHost 의 PredictiveBackHandler 까지 실제로 닿았다는 증거 — 진행 중 아래
        // 화면을 미리 꺼내 보여 준 흔적이다. 닿지 않았다면 `dispatchOnBackCancelled` 는
        // in-progress 콜백이 없어 통째로 no-op 이라 visibleEntries 가 그대로였을 것이다.
        //
        // 주의: Robolectric 은 프레임을 돌리지 않아 이 «미리보기» 가 끝내 사라지지 않는다.
        // 기기에서 사라지는지는 이 단언의 범위 밖이다 — docs/qa/predictive-back.md 가 본다.
        assertTrue(
            "취소 후 visibleEntries=${visibleRoutes()}",
            visibleRoutes().contains("TimeLetterHomeRoute"),
        )
    }

    @Test
    fun `predictive back 을 끝까지 진행하면 한 칸만 pop 된다`() {
        startOnTimeLetterDraft()

        beginGesture()
        composeRule.runOnIdle { harness.backDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(
            listOf("NavHostRoot", "Home", "TimeLetter", "TimeLetterHomeRoute"),
            routes(),
        )
    }

    @Test
    fun `취소한 뒤 다시 시작한 predictive back 도 정상적으로 pop 된다`() {
        startOnTimeLetterDraft()

        beginGesture()
        composeRule.runOnIdle { harness.backDispatcher.dispatchOnBackCancelled() }
        composeRule.waitForIdle()

        beginGesture()
        composeRule.runOnIdle { harness.backDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(
            listOf("NavHostRoot", "Home", "TimeLetter", "TimeLetterHomeRoute"),
            routes(),
        )
    }

    @Test
    fun `탭 루트에서 시작한 predictive back 은 홈 탭까지만 내려간다`() {
        startOnTimeLetterDraft()
        composeRule.runOnIdle { harness.timeLetterActions.onDraftBack() }

        beginGesture()
        composeRule.runOnIdle { harness.backDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        // 타임레터 그래프 엔트리까지 함께 빠지고 홈이 남는다 — 앱을 벗어나지 않는다.
        assertEquals(listOf("NavHostRoot", "Home"), routes())
    }

    private fun backEvent(progress: Float): BackEventCompat =
        BackEventCompat(
            touchX = 0f,
            touchY = 100f,
            progress = progress,
            swipeEdge = BackEventCompat.EDGE_LEFT,
        )

    private companion object {
        val DRAFT_STACK =
            listOf("NavHostRoot", "Home", "TimeLetter", "TimeLetterHomeRoute", "TimeLetterDraftRoute")
    }
}
