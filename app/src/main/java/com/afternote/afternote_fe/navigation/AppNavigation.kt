package com.afternote.afternote_fe.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.afternote.afternote_fe.screen.HomeTabScreen
import com.afternote.afternote_fe.screen.HomeTabViewModel
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.author.navigation.afternoteNavGraph
import com.afternote.feature.mindrecord.presentation.navigation.mindRecordNavGraph
import com.afternote.feature.onboarding.presentation.navigation.onboardingNavGraph
import com.afternote.feature.setting.presentation.SettingScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    startDestination: Route,
    modifier: Modifier = Modifier,
    appState: AppState = rememberAfternoteAppState(),
) {
    val navEntry by appState.navController.currentBackStackEntryAsState()
    val currentDestination = navEntry?.destination
    val showBottomBar = appState.shouldShowBottomBar(currentDestination)
    val currentTab = appState.getCurrentNavTab(currentDestination)

    NavigationDebugLogger(navEntry, currentDestination, showBottomBar, currentTab)

    val onboardingNavActions = rememberOnboardingNavActions(appState.navController)
    val mindRecordNavActions = rememberMindRecordNavActions(appState.navController)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val afternoteNavActions =
        rememberAfternoteNavActions(appState) { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }

    Scaffold(
        modifier = modifier,
        containerColor = AfternoteDesign.colors.gray1,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets =
            WindowInsets.systemBars.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
            ),
        bottomBar = {
            if (showBottomBar) {
                BottomBar(
                    onTabClick = { item -> appState.navigateToBottomBarRoute(item.route) },
                    selectedNavTab = currentTab,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding),
            navController = appState.navController,
            startDestination = startDestination,
        ) {
            onboardingNavGraph(
                graphScopedParentEntry = {
                    appState.navController.getBackStackEntry<Route.Onboarding>()
                },
                actions = onboardingNavActions,
            )
            composable<Route.Home> {
                val viewModel: HomeTabViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val homeTabActions =
                    rememberHomeTabActions(
                        appState = appState,
                        onRetryLoad = { viewModel.loadHomeSummary(isRefresh = true) },
                    )
                HomeTabScreen(
                    uiState = uiState,
                    actions = homeTabActions,
                )
            }
            composable<Route.Setting> {
                SettingScreen(
                    onLogoutSuccess = {
                        appState.navController.navigate(Route.Onboarding) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            mindRecordNavGraph(actions = mindRecordNavActions)
            composable<Route.TimeLetter> { TimeletterScreen() }
            afternoteNavGraph(
                graphScopedParentEntry = {
                    appState.navController.getBackStackEntry<Route.Afternote>()
                },
                actions = afternoteNavActions,
            )
        }
    }
}
