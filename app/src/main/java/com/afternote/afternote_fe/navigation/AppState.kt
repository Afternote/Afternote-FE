package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afternote.core.ui.Route
import com.afternote.core.ui.component.bottombar.BottomNavTab

// TODO:검토
@Stable
class AppState(
    val navController: NavHostController,
// 추후 여기에 val snackbarHostState: SnackbarHostState 등을 추가합니다.
) {
    // 현재 백스택 목적지
    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    // 바텀바가 선택해야 할 현재 라우트 계산
    val currentRoute: Route?
        @Composable get() =
            BottomNavTab.entries
                .firstOrNull { currentDestination?.hasRoute(it.route::class) == true }
                ?.route

    // 바텀바 아이템 클릭 시 이동하는 네비게이션 로직
    fun navigateToBottomBarRoute(route: Route) {
        navController.navigate(route) {
            popUpTo<Route.Home> { saveState = true }
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
