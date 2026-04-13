package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

// 컴포즈 엔진은 커스텀 클래스에 대해 변경 여부를 확신할 수 없어 리컴포지션 스킵 불가
// 이 클래스는 상태 변경 시 컴포즈에 알려 줄 것을 보장 매번 다시 그릴 필요 없음
// 이 어노테이션을 통해 상태 변경을 알려 줄 때가 아니면 리컴포지션하지 않게 함
@Stable
class AppState(
    val navController: NavHostController,
) {
    // 현재 백스택 엔트리의 상태를 가져와 엔트리의 목적지를 저장
    val currentDestination: NavDestination?
        // 어노테이션을 붙여 get() = 뒤 전체 코드를 컴포저블 함수로 만들어 상태를 다룰 수 있게 해 줌
        // value가 바뀔 때마다 currentDestination를 호출한 컴포저블 함수는 리컴포지션
        // 리컴포지션시 커스텀게터가 navController.currentBackStackEntryAsState()부터 재호출하기 때문에 최신 엔트리의 데스티네이션을 줌
        // 게터가 없으면 currentDestination에 처음 저장했던 값만 계속 주게 됨
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentNavTab: BottomNavTab
        @Composable get() =
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
    // 여기에 없는 화면(상세, 에디터 등)에서는 바텀바가 숨겨짐
    private val bottomBarRoutes =
        setOf(
            Route.Home::class,
            Route.MindRecord::class,
            Route.TimeLetter::class,
            AfternoteRoute.AfternoteHomeRoute::class,
            AfternoteRoute.FingerprintLoginRoute::class,
        )

    val shouldShowBottomBar: Boolean
        @Composable get() =
            bottomBarRoutes.any { route ->
                currentDestination?.hasRoute(route) == true
            }

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
            restoreState = true
        }
    }
}

@Composable
fun rememberAfternoteAppState(navController: NavHostController = rememberNavController()): AppState =
    remember(navController) {
        AppState(navController)
    }
