package com.afternote.afternote_fe.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.afternote.core.ui.Route
import com.afternote.core.ui.scaffold.bottombar.BottomBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavGraphParams
import com.afternote.feature.afternote.presentation.author.navigation.afternoteNavGraph
import com.afternote.feature.mindrecord.presentation.screen.sender.HomeScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    appState: AppState = rememberAfternoteAppState(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = AfternoteDesign.colors.gray1,
        contentWindowInsets =
            WindowInsets.systemBars.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        bottomBar = {
            if (appState.shouldShowBottomBar) {
                BottomBar(
                    onTabClick = { item -> appState.navigateToBottomBarRoute(item.route) },
                    selectedNavTab = appState.currentNavTab,
                )
            }
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
                params =
                    AfternoteNavGraphParams(
                        navController = appState.navController,
                        onNavTabSelected = { tab -> appState.navigateToBottomBarRoute(tab.route) },
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppNavigationPreview() {
    AfternoteTheme {
        AppNavigation()
    }
}
