package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.afternote.core.common.deeplink.NavigationTarget
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute

// 컴포즈 엔진은 커스텀 클래스에 대해 변경 여부를 확신할 수 없어 리컴포지션 스킵 불가
// 이 클래스는 상태 변경 시 컴포즈에 알려 줄 것을 보장 매번 다시 그릴 필요 없음
// 이 어노테이션을 통해 상태 변경을 알려 줄 때가 아니면 리컴포지션하지 않게 함
@Stable
class AppState(
    val navController: NavHostController,
) {
    /**
     * @param isAfternoteStackAtRoot 애프터노트 로컬 Nav3 스택이 바닥(지문 관문 또는 홈)인지.
     *   그 그래프는 이제 Nav2 destination 이 [Route.Afternote] 하나뿐이라 destination 만으로는
     *   상세·에디터가 쌓였는지 알 수 없다 — 깊이를 아는 host 가 올려 준다 (#1698).
     */
    fun shouldShowBottomBar(
        currentDestination: NavDestination?,
        isAfternoteStackAtRoot: Boolean,
    ): Boolean =
        if (currentDestination?.hasRoute(Route.Afternote::class) == true) {
            isAfternoteStackAtRoot
        } else {
            bottomBarRoutes.any { route ->
                currentDestination?.hasRoute(route) == true
            }
        }

    fun getCurrentNavTab(currentDestination: NavDestination?): BottomNavTab =
        // 현재 데스티네이션에 대해 탭을 순회하며
        BottomNavTab.entries.firstOrNull { tab ->
            // <기본 동작>
            // 현재 데스티네이션부터 상위 그래프로 거슬러 올라감
            // 계층 구조 내 현재 탭의 라우트를 가진 노드(데스티네이션)가 있는지 확인
            // <여기서 쓰는 목적>
            // 탭의 라우트는 노드 단위인 것도 있지만 서브 그래프 단위인 것도 있음
            // 현재 화면이 해당 서브 그래프의 자식으로 소속되어 있는지 확인
            currentDestination?.hierarchy?.any { destination ->
                destination.hasRoute(tab.route::class)
            } == true
        } ?: BottomNavTab.HOME

    // 바텀바를 보여줄 화면 목록
    // 여기에 없는 화면(상세, 에디터 등)에서는 바텀바가 숨겨짐.
    // 애프터노트는 로컬 스택 깊이로 판정하므로 이 목록이 아니라 shouldShowBottomBar 가 직접 다룬다.
    private val bottomBarRoutes =
        setOf(
            Route.Home::class,
            Route.MindRecord::class,
            Route.TimeLetter::class,
            TimeLetterRoute.TimeLetterHomeRoute::class,
        )

    fun navigateToBottomBarRoute(route: Route) {
        navController.navigate(route) {
            // Route.Home은 바텀바가 보이는 모든 시점에서 반드시 백스택에 존재한다.
            // findStartDestination()을 쓰면 startDestination이 Route.Onboarding일 때
            // 로그인 이후 백스택에 없는 온보딩 화면을 가리켜 popUpTo가 무시되는 버그가 발생한다.
            popUpTo<Route.Home> {
                saveState = true
                // inclusive = false — Home을 백스택에 유지한다 (기본값이나 의도를 명시)
            }
            launchSingleTop = true
            // 애프터노트 서브그래프의 start는 인증 화면이다. restoreState = true이면
            // 저장된 홈 등으로 복원되어 인증을 건너뛴다. 진입점은 피처 NavGraph가 단일로 결정한다.
            restoreState = route != Route.Afternote
        }
    }

    /**
     * 엔진 중립 링크 목적지를 이 루트 그래프의 이동으로 옮긴다 (#924).
     *
     * 링크 계약([NavigationTarget])은 서버·브라우저와 공유하는 외부 계약이라 navigation 엔진과
     * 수명이 다르다. 그 둘을 잇는 유일한 자리가 여기다 — 루트가 `NavDisplay` 로 바뀌면(#1702)
     * 계약은 그대로 두고 이 함수만 갈린다.
     *
     * 탭 목적지 둘은 [navigateToBottomBarRoute] 를 그대로 탄다. 특히 애프터노트 홈은 그 함수가
     * `restoreState` 를 끄는 유일한 라우트라, 저장된 스택으로 복원되어 **지문 관문을 건너뛰는**
     * 일이 링크 진입에서도 일어나지 않는다.
     *
     * 나머지 둘은 사용자가 보고 있던 화면 위에 쌓는다. 링크를 눌러 들어온 뒤의 뒤로가기는 하던
     * 일로 돌아가는 것이 맞고, 콜드 스타트면 그 아래가 홈이라 결과가 같다. 같은 링크를 연달아
     * 눌러도 같은 화면이 겹쳐 쌓이지 않도록 맨 위가 같은 화면이면 쌓지 않는다([pushUnlessOnTop]).
     */
    fun navigateToAppLinkTarget(target: NavigationTarget) {
        when (target) {
            NavigationTarget.Home -> {
                navigateToBottomBarRoute(Route.Home)
            }

            NavigationTarget.AfternoteHome -> {
                navigateToBottomBarRoute(Route.Afternote)
            }

            NavigationTarget.DailyQuestionCompose -> {
                pushUnlessOnTop(MindRecordRoute.DailyQuestionWriteRoute())
            }

            NavigationTarget.NotificationSettings -> {
                pushUnlessOnTop(SettingRoute.NotificationRoute)
            }
        }
    }

    /**
     * 맨 위가 **인자까지 같은** 라우트면 그대로 두고, 아니면 그 위에 쌓는다.
     *
     * `launchSingleTop` 을 쓰지 않는 이유: 그 옵션은 목적지 종류만 비교한다. 같은 종류가 맨 위에
     * 있으면 인자만 새것으로 바꾼 entry 로 갈아 끼우는데, entry id 가 이어져 ViewModel 은 처음
     * 인자를 쥔 채 남는다. 인자를 ViewModel 이 `SavedStateHandle` 로 읽는 화면은 이전 인자의
     * 화면을 계속 보여 준다 — 답변 수정(`DailyQuestionWriteRoute(answerId = …)`) 중에 데일리질문
     * 링크가 오면 새 작성 화면 대신 수정 화면이 남는다.
     */
    private inline fun <reified T : Any> pushUnlessOnTop(route: T) {
        val top = navController.currentBackStackEntry
        if (top != null && top.destination.hasRoute<T>() && top.toRoute<T>() == route) return
        navController.navigate(route)
    }
}

@Composable
fun rememberAfternoteAppState(navController: NavHostController = rememberNavController()): AppState =
    remember(navController) {
        AppState(navController)
    }
