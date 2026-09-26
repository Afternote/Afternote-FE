package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
 * 링크 목적지가 **어느 화면에 도착하는지**를 루트 백스택으로 못박는다 (#924).
 *
 * 파서 테스트는 URL 이 어떤 [NavigationTarget] 이 되는지까지만 본다. 그 목적지가 실제로 어느
 * 라우트로 옮겨지는지는 앱 모듈의 배선이라 이 층에서만 잡힌다 — 목적지 둘을 서로 바꿔 붙여도
 * 컴파일은 통과한다.
 *
 * 애프터노트 홈이 특히 이 층의 판정거리다. 그 목적지는 지문 관문을 지나야 하는데, 관문은 로컬
 * 스택의 시작 화면이라 **저장된 스택으로 복원되면 건너뛰어진다**. 그래서 탭 저장/복원을 껐는지를
 * 「탭을 떠났다 링크로 돌아와도 저장된 화면이 복원되지 않는다」로 잰다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class AppLinkTargetBackStackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun startAtHome() {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = Route.Home) { harness = it }
        }
        composeRule.waitForIdle()
    }

    private fun resume(target: NavigationTarget) {
        composeRule.runOnIdle { harness.appState.navigateToAppLinkTarget(target) }
    }

    private fun routes(): List<String> = composeRule.runOnIdle { harness.navController.backStackRouteNames() }

    @Test
    fun `사이트 루트는 홈에 머문다`() {
        startAtHome()

        resume(NavigationTarget.Home)

        assertEquals(listOf("NavHostRoot", "Home"), routes())
    }

    @Test
    fun `애프터노트 홈은 홈 위에 애프터노트 그래프를 올린다`() {
        startAtHome()

        resume(NavigationTarget.AfternoteHome)

        assertEquals(listOf("NavHostRoot", "Home", "Afternote"), routes())
    }

    /**
     * 지문 관문 회귀. 애프터노트 탭에서 화면 상태를 만들어 두고 떠났다가 **링크로** 돌아왔을 때
     * 그 상태가 복원되면, 관문(로컬 스택의 시작 화면)까지 함께 건너뛴 것이다.
     */
    @Test
    fun `링크로 연 애프터노트는 저장된 스택을 복원하지 않는다`() {
        startAtHome()
        resume(NavigationTarget.AfternoteHome)
        composeRule.onNodeWithText("Afternote#0").performClick()
        composeRule.onNodeWithText("Afternote#1").assertExists()

        composeRule.runOnIdle { harness.appState.navigateToBottomBarRoute(Route.Home) }
        resume(NavigationTarget.AfternoteHome)

        composeRule.onNodeWithText("Afternote#0").assertExists()
    }

    /**
     * 중첩 그래프의 시작 화면(설정 홈)은 끼지 않는다 — 링크는 그 화면을 지나온 적이 없다. 그래서
     * 여기서의 뒤로가기는 설정 홈이 아니라 그 아래(콜드 스타트면 홈)로 간다.
     */
    @Test
    fun `알림 설정은 설정 그래프를 지나 알림 화면에 선다`() {
        startAtHome()

        resume(NavigationTarget.NotificationSettings)

        assertEquals(listOf("NavHostRoot", "Home", "Setting", "NotificationRoute"), routes())
        assertEquals(true, composeRule.runOnIdle { harness.navController.popBackStack() })
        assertEquals(listOf("NavHostRoot", "Home"), routes())
    }

    @Test
    fun `데일리질문 작성은 홈 위에 바로 선다`() {
        startAtHome()

        resume(NavigationTarget.DailyQuestionCompose)

        assertEquals(listOf("NavHostRoot", "Home", "DailyQuestionWriteRoute"), routes())
    }

    /** 같은 링크를 연달아 눌러도 같은 화면이 겹쳐 쌓이지 않는다. */
    @Test
    fun `같은 링크를 두 번 열어도 화면이 겹쳐 쌓이지 않는다`() {
        startAtHome()

        resume(NavigationTarget.DailyQuestionCompose)
        resume(NavigationTarget.DailyQuestionCompose)

        assertEquals(listOf("NavHostRoot", "Home", "DailyQuestionWriteRoute"), routes())
    }
}
