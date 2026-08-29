package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
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

/**
 * 마인드레코드의 「한 칸 뒤로」가 **실제 NavHost 에서** 백스택을 원래대로 돌려놓는지 본다 (#1311).
 *
 * 화면별로 나뉘어 있던 단순 `popBackStack()` 5개를 [MindRecordNavActions.popBack] 하나로 합쳤다.
 * 명령이 하나가 됐으니, 다섯 자리 중 하나라도 잘못 매핑되면(예: 두 칸 pop, 다른 그래프로 이동)
 * 여기서 잡혀야 한다. 그래서 **명령을 직접 부르는 것이 아니라 각 화면 라우트로 실제 이동한 뒤**
 * 돌아온 자리를 단언한다.
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

    @Test
    fun popBack_returnsToMindRecordHomeFromEveryMindRecordDestination() {
        // 종전에 화면마다 따로 있던 다섯 자리를 전부 태운다 — 하나라도 다른 곳으로 가면 여기서 갈린다.
        listOf<Any>(
            Route.MemorySpace,
            Route.ReceiverMindRecord,
            MindRecordRoute.DailyQuestionWriteRoute(),
            MindRecordRoute.DiaryWriteRoute(),
            MindRecordRoute.DraftListRoute,
        ).forEach { destination ->
            composeRule.runOnIdle { navController.navigate(destination) }
            composeRule.waitForIdle()
            assertTrue(
                "$destination 으로 이동하지 못했다",
                composeRule.runOnIdle { navController.currentDestination?.hasRoute<Route.MindRecord>() } == false,
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
    fun writeSubmitSuccess_popsExactlyOneEntry() {
        // 제출 성공도 같은 명령에 붙였다 — «한 칸» 이 아니라 그래프째 비우면 홈까지 사라진다.
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
