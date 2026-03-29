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

// 컴포즈 엔진은 커스텀 클래스에 대해 변경 여부를 확신할 수 없어 리컴포지션 스킵 불가
// 이 클래스는 상태 변경 시 컴포즈에 알려 줄 것을 보장 매번 다시 그릴 필요 없음
// 이 어노테이션을 통해 상태 변경을 알려 줄 때가 아니면 리컴포지션하지 않게 함
@Stable
class AppState(
    val navController: NavHostController,
) {
    // 현재 백스택 엔트리의 상태를 가져와 엔트리의 목적지를 저장
    val currentDestination: NavDestination?
        // 상태 관찰은 컴포저블 스코프 내에서만 가능하므로 어노테이션을 붙여 컴포저블 함수로서 컴포지션에 등록
        // 엔트리의 상태가 바뀔 때마다 get()을 컴포즈 엔진으로 하여금 다시 그리게 한다
        // 커스텀 게터는 변수를 읽을 때마다 최신 정보가 반영된 변수를 읽게 해 준다
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentRoute: Route?
        @Composable get() =
            BottomNavTab.entries
                // 조건을 만족하는 첫 번째 요소를 가져 오고 없으면 널 반환
                .firstOrNull { currentDestination?.hasRoute(it.route::class) == true }
                ?.route

    fun navigateToBottomBarRoute(route: Route) {
        navController.navigate(route) {
            popUpTo<Route.Home> { saveState = true } // 제거할 데스티네이션의 상태를 저장
            launchSingleTop = true // 푸시되는 데스티네이션이 탑 데스티네이션과 라우트가 같다면 엔트리를 새로 생성하지 않음
            restoreState = true // 이동할 라우트가 saveState한 적 있다면 복원
        }
    }
}

@Composable
fun rememberAfternoteAppState(navController: NavHostController = rememberNavController()): AppState =
    remember(navController) {
        AppState(navController)
    }
