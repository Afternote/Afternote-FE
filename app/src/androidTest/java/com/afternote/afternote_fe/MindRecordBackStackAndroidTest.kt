package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.navigation.rememberMindRecordNavActions
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordNavActions
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

/**
 * 마인드레코드의 「한 칸 뒤로」가 **실제 NavHost 에서** 백스택을 원래대로 돌려놓는지 본다 (#1311).
 *
 * 화면별로 나뉘어 있던 단순 `popBackStack()` 5개를 [MindRecordNavActions.popBack] 하나로 합쳤다.
 *
 * 두 층을 나눠 본다.
 * - `screenBackButton_…` — **화면의 뒤로가기를 실제로 눌러** `mindRecordNavGraph` 의 매핑을 지난다.
 *   매핑 오배선은 컴파일이 잡지 못하므로(#1311 리뷰 실측) 이 층이 없으면 감시하는 것이 없다.
 * - `popBack_…` 둘 — 명령 자체의 계약(어느 목적지에서든 한 칸, 잔재 없음).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MindRecordBackStackAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private lateinit var navController: TestNavHostController

    /** 라우트 객체가 NavHost 에 등록될 때의 route 문자열. 인자가 있는 라우트는 패턴이 붙는다. */
    private fun routeOf(destination: Any): String? =
        composeRule.runOnIdle {
            navController.graph.find { node -> node.hasRoute(destination::class) }?.route
        }

    private fun label(resId: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    /** 화면마다 뒤로가기 라벨이 달라 둘 중 실재하는 쪽을 누른다. */
    private fun clickBackAffordance() {
        val labels =
            listOf(
                label(CoreUiR.string.core_ui_content_description_back),
                label(MindRecordR.string.mindrecord_memory_space_back),
            )
        val target =
            labels.firstOrNull { candidate ->
                composeRule.onAllNodesWithContentDescription(candidate).fetchSemanticsNodes().isNotEmpty()
            }
        requireNotNull(target) { "뒤로가기 어포던스를 찾지 못했다 — 라벨 후보: $labels" }
        composeRule.onAllNodesWithContentDescription(target).onFirst().performClick()
    }

    private lateinit var actions: MindRecordNavActions

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activityRule.scenario.onActivity { activity ->
            navController =
                TestNavHostController(activity).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            activity.setContent {
                AfternoteTheme {
                    actions = rememberMindRecordNavActions(navController)
                    AppNavigation(
                        startDestination = Route.MindRecord,
                        appState = AppState(navController),
                    )
                }
            }
        }
        awaitRoute<Route.MindRecord>()
    }

    /**
     * **화면의 뒤로가기를 실제로 눌러** 매핑을 태운다 (#1311 리뷰).
     *
     * 명령(`actions.popBack()`)을 직접 부르면 이 PR 이 손댄 `mindRecordNavGraph` 의 매핑 줄을
     * 하나도 지나가지 않는다 — 매핑을 오염시켜도 컴파일이 통과하므로 그때 잡을 것이 없다.
     * 그래서 각 화면의 뒤로가기 버튼을 눌러 `onBackClick = actions::popBack` 를 실제로 지난다.
     */
    @Test
    fun screenBackButton_returnsToMindRecordHomeFromEveryMindRecordDestination() {
        listOf<Any>(
            Route.MemorySpace,
            Route.ReceiverMindRecord,
            MindRecordRoute.DailyQuestionWriteRoute(),
            MindRecordRoute.DiaryWriteRoute(),
            MindRecordRoute.DraftListRoute,
        ).forEach { destination ->
            composeRule.runOnIdle { navController.navigate(destination) }
            composeRule.waitForIdle()

            // 화면이 그린 뒤로가기다 — 명령을 직접 부르지 않는다.
            // 추억 공간만 자체 라벨(BackPill)을 쓰므로 둘 중 있는 쪽을 누른다.
            clickBackAffordance()

            awaitRoute<Route.MindRecord>()
            assertEquals(
                "$destination 에서 돌아온 뒤 백스택에 잔재가 남았다",
                null,
                composeRule.runOnIdle { navController.previousBackStackEntry?.destination?.route },
            )
        }
    }

    @Test
    fun popBack_returnsToMindRecordHomeFromEveryMindRecordDestination() {
        // 명령 자체의 계약 — «한 칸 뒤로» 가 어느 목적지에서든 홈으로 돌아오고 잔재를 남기지 않는다.
        listOf<Any>(
            Route.MemorySpace,
            Route.ReceiverMindRecord,
            MindRecordRoute.DailyQuestionWriteRoute(),
            MindRecordRoute.DiaryWriteRoute(),
            MindRecordRoute.DraftListRoute,
        ).forEach { destination ->
            composeRule.runOnIdle { navController.navigate(destination) }
            composeRule.waitForIdle()
            // «홈이 아니다» 만 보면 엉뚱한 목적지도 통과한다 — 실제로 그 목적지인지 본다 (#1311 리뷰).
            assertEquals(
                "$destination 으로 이동하지 못했다",
                routeOf(destination),
                composeRule.runOnIdle { navController.currentDestination?.route },
            )

            composeRule.runOnIdle { actions.popBack() }
            awaitRoute<Route.MindRecord>()
            // 뒤로 온 뒤 백스택 위에 남은 것이 없어야 한다 — 남으면 다음 뒤로가기가 죽은 화면을 연다.
            assertEquals(
                "$destination 에서 돌아온 뒤 백스택에 잔재가 남았다",
                null,
                composeRule.runOnIdle { navController.previousBackStackEntry?.destination?.route },
            )
        }
    }

    @Test
    fun popBack_popsExactlyOneEntryInsteadOfClearingTheGraph() {
        // 이름을 실체에 맞춘다 — 이 테스트는 제출 성공 경로를 지나가지 않는다 (#1311 리뷰).
        // 제출 성공도 같은 명령(`onSubmitSuccess = actions::popBack`)에 붙였으므로, 여기서 보는
        // 것은 그 명령의 성질이다: «한 칸» 이 아니라 그래프째 비우면 홈까지 사라진다.
        composeRule.runOnIdle { navController.navigate(MindRecordRoute.DiaryWriteRoute()) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { navController.navigate(MindRecordRoute.DraftListRoute) }
        composeRule.waitForIdle()

        composeRule.runOnIdle { actions.popBack() }

        // 임시저장 목록에서 한 칸 뒤로면 작성 화면이다 — 홈이 아니다.
        awaitRoute<MindRecordRoute.DiaryWriteRoute>()
    }

    private inline fun <reified T : Any> awaitRoute() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            navController.currentDestination?.hasRoute<T>() == true
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
