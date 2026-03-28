package com.afternote.afternote_fe.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afternote.core.ui.BottomBar
import com.afternote.core.ui.BottomNavItem
import com.afternote.core.ui.R
import com.afternote.core.ui.Route
import com.afternote.feature.afternote.presentation.AfternoteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen

private val bottomNavItems =
    listOf(
        BottomNavItem("홈", R.drawable.core_ui_ic_home, Route.Home),
        BottomNavItem("기록", R.drawable.core_ui_ic_mindrecord, Route.MindRecord),
        BottomNavItem("타임레터", R.drawable.core_ui_ic_mail, Route.TimeLetter),
        BottomNavItem("노트", R.drawable.core_ui_ic_note, Route.Afternote),
    )

private fun NavController.navigateTo(route: Route) {
    val builder: NavOptionsBuilder.() -> Unit = {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
    when (route) {
        is Route.Home -> navigate(route, builder = builder)
        is Route.MindRecord -> navigate(route, builder = builder)
        is Route.TimeLetter -> navigate(route, builder = builder)
        is Route.Afternote -> navigate(route, builder = builder)
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentRoute: Route? =
        when {
            currentDestination?.hasRoute(Route.Home::class) == true -> Route.Home
            currentDestination?.hasRoute(Route.MindRecord::class) == true -> Route.MindRecord
            currentDestination?.hasRoute(Route.TimeLetter::class) == true -> Route.TimeLetter
            currentDestination?.hasRoute(Route.Afternote::class) == true -> Route.Afternote
            else -> null
        }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomBar(
                items = bottomNavItems,
                isSelected = { item -> item.route == currentRoute },
                onItemClick = { item -> navController.navigateTo(item.route) },
            )
        },
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = Route.Home,
        ) {
            composable<Route.Home> { HomeScreen() } // TODO: 진짜 homeScreen 구현 후 교체
            composable<Route.MindRecord> { HomeScreen() }
            composable<Route.TimeLetter> { TimeletterScreen() }
            composable<Route.Afternote> { AfternoteScreen() }
        }
    }
}
