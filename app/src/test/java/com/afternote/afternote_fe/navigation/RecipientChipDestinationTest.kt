package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.toRoute
import com.afternote.core.ui.Route
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 홈 «수신인 지정 미완료» 칩이 **어디로 가는지**를 못박는다 (#506 · #1695).
 *
 * Nav2 시절엔 칩이 `SettingRoute.RecipientRegisterRoute` 를 루트에 직접 찍어 백스택 이름만으로
 * 목적지가 보였다. 설정이 로컬 스택이 된 뒤로는 루트에 [Route.Setting] 한 칸만 쌓이고 등록 화면
 * 진입은 그 인자([Route.Setting.startWithRecipientRegistration])가 나른다 — 인자를 버리는
 * [backStackRouteNames] 로는 설정 홈 진입과 구분되지 않으므로 [toRoute] 로 값까지 꺼내 단언한다.
 * 그 인자로 host 가 실제 첫 화면을 고르는 몫은 `SettingImplementedCoverageAndroidTest` 가 본다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class RecipientChipDestinationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun startAtHome() {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = Route.Home) { harness = it }
        }
        composeRule.waitForIdle()
    }

    private fun currentSetting(): Route.Setting =
        composeRule.runOnIdle {
            harness.navController.currentBackStackEntry!!.toRoute<Route.Setting>()
        }

    @Test
    fun `수신인 칩은 설정 host 한 칸을 쌓고 등록 화면 시작을 인자로 싣는다`() {
        startAtHome()

        composeRule.runOnIdle { harness.homeActions.onRecipientChipClick() }

        assertEquals(
            listOf("NavHostRoot", "Home", "Setting"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
        assertEquals(true, currentSetting().startWithRecipientRegistration)
    }

    @Test
    fun `설정 메뉴 진입은 같은 host 에 설정 홈 시작으로 들어간다`() {
        startAtHome()

        composeRule.runOnIdle { harness.homeActions.onSettingClick() }

        assertEquals(false, currentSetting().startWithRecipientRegistration)
    }
}
