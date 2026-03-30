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
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import kotlin.reflect.KClass

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

    var lastNavTab: BottomNavTab = BottomNavTab.HOME
    val currentNavTab: BottomNavTab
        @Composable get() {
            val matched =
                BottomNavTab.entries.firstOrNull {
                    currentDestination?.hasRoute(it.route::class as KClass<out Any>) == true
                }
            if (matched == null) {
                return lastNavTab
            }
            lastNavTab = matched
            return matched
        }
//    val currentRoute: Route?
//            BottomNavTab.entries
//                // 조건을 만족하는 첫 번째 요소를 가져 오고 없으면 널 반환
//                .firstOrNull { currentDestination?.hasRoute(it.route::class) == true }
//                ?.route
//            return

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
