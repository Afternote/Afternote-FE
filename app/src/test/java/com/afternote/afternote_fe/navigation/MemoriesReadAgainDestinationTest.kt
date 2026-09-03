package com.afternote.afternote_fe.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.toRoute
import com.afternote.core.ui.Route
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 홈 MEMORIES 의 「그날의 기록 다시 읽기」가 **어디로 가는지**를 못박는다 (#793).
 *
 * 카드 쪽 회귀(`MemoriesReadAgainTest`)는 0건에서 버튼이 없다는 것과 두 콜백이 서로 다르다는
 * 것까지만 센다 — 콜백이 실제로 어떤 목적지를 여는지는 앱 모듈의 배선에 있어서 그 층에서는
 * 잡히지 않는다. 그래서 반대 방향, 즉 **유효한 기록 id 를 받았을 때 그 id 그대로
 * [MindRecordRoute.RecordDetailRoute] 에 도착하는지**를 여기서 고정한다.
 *
 * [MindRecordRoute.RecordDetailRoute.isDiary] 가 `false` 인 것이 계약의 절반이다. 카드가 싣는
 * 것은 가장 최근 **데일리질문 답변** 한 건이라, 이 값이 뒤집히면 상세가 일기를 찾으러 가서
 * 빈 화면이 된다. 인자를 버리는 [backStackRouteNames] 로는 이 뒤집힘이 보이지 않으므로
 * [toRoute] 로 값까지 꺼내 단언한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class MemoriesReadAgainDestinationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var harness: NavBackStackHarness

    private fun startAtHome() {
        composeRule.setContent {
            SkeletonAppNavigation(startDestination = Route.Home) { harness = it }
        }
        composeRule.waitForIdle()
    }

    private fun currentRecordDetail(): MindRecordRoute.RecordDetailRoute =
        composeRule.runOnIdle {
            harness.navController.currentBackStackEntry!!.toRoute<MindRecordRoute.RecordDetailRoute>()
        }

    @Test
    fun `다시 읽기는 받은 기록 id 를 그대로 실어 기록 상세로 간다`() {
        startAtHome()

        composeRule.runOnIdle { harness.homeActions.onMemoriesRecordDetailClick(RECORD_ID) }

        assertEquals(
            listOf("NavHostRoot", "Home", "RecordDetailRoute"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
        assertEquals(RECORD_ID, currentRecordDetail().recordId)
    }

    @Test
    fun `다시 읽기가 여는 상세는 일기가 아니라 데일리질문 답변이다`() {
        startAtHome()

        composeRule.runOnIdle { harness.homeActions.onMemoriesRecordDetailClick(RECORD_ID) }

        assertEquals(false, currentRecordDetail().isDiary)
    }

    @Test
    fun `MEMORIES 섹션 자체는 추억 공간으로 가 상세와 목적지가 갈린다`() {
        startAtHome()

        composeRule.runOnIdle { harness.homeActions.onMemoriesSectionClick() }

        assertEquals(
            listOf("NavHostRoot", "Home", "MemorySpace"),
            composeRule.runOnIdle { harness.navController.backStackRouteNames() },
        )
    }

    private companion object {
        /** 0 이나 1 이면 기본값·인덱스와 구분되지 않아, 그대로 실렸는지 보이는 값을 쓴다. */
        const val RECORD_ID = 4_213L
    }
}
