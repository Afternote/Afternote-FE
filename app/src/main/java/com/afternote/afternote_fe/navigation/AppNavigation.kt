package com.afternote.afternote_fe.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.afternote.core.ui.Route
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.feature.afternote.presentation.author.navigation.afternoteNavGraph
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen

// TODO:검토
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    appState: AppState = rememberAfternoteAppState(),
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomBar(
                onTabClick = { item -> appState.navigateToBottomBarRoute(item.route) },
                selectedNavTab = appState.currentNavTab,
            )
        },
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = appState.navController,
            startDestination = Route.Home,
        ) {
            composable<Route.Home> { HomeScreen() } // TODO: 진짜 homeScreen 구현 후 교체
            composable<Route.MindRecord> { HomeScreen() }
            composable<Route.TimeLetter> { TimeletterScreen() }
            afternoteNavGraph(
                navController = appState.navController,
                onNavTabSelected = { tab -> appState.navigateToBottomBarRoute(tab.route) },
            )
        }
    }
}
