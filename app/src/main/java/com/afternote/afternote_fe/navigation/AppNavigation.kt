package com.afternote.afternote_fe.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afternote.core.ui.BottomBar
import com.afternote.core.ui.BottomNavItem
import com.afternote.core.ui.R
import com.afternote.feature.afternote.presentation.AfternoteScreen
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen

private val bottomNavItems =
    listOf(
        BottomNavItem("홈", R.drawable.ic_home, AppDestination.Home.route),
        BottomNavItem("기록", R.drawable.ic_mindrecord, AppDestination.Mindrecord.route),
        BottomNavItem("타임레터", R.drawable.ic_mail, AppDestination.Timeletter.route),
        BottomNavItem("노트", R.drawable.ic_note, AppDestination.Afternote.route),
    )

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            BottomBar(
                items = bottomNavItems,
                currentRoute = currentRoute,
                onItemClick = { item ->
                    navController.navigate(item.route.value) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route.value,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Home.route.value) { HomeScreen() }
            // TODO: MindrecordScreen 구현 후 교체
            composable(AppDestination.Mindrecord.route.value) { HomeScreen() }
            composable(AppDestination.Timeletter.route.value) { TimeletterScreen() }
            composable(AppDestination.Afternote.route.value) { AfternoteScreen() }
        }
    }
}
