package com.afternote.afternote_fe.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.afternote.afternote_fe.screen.HomeTabScreen
import com.afternote.afternote_fe.screen.HomeTabViewModel
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavGraphParams
import com.afternote.feature.afternote.presentation.author.navigation.afternoteNavGraph
import com.afternote.feature.mindrecord.presentation.navigation.mindRecordNavGraph
import com.afternote.feature.onboarding.presentation.navigation.onboardingNavGraph
import com.afternote.feature.setting.presentation.SettingScreen
import com.afternote.feature.timeletter.presentation.screen.sender.TimeletterScreen

@Composable
fun AppNavigation(
    startDestination: Route,
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
            startDestination = startDestination,
        ) {
            onboardingNavGraph(
                navController = appState.navController,
                onOnboardingComplete = {
                    appState.navController.navigate(Route.Home) {
                        popUpTo<Route.Onboarding> { inclusive = true }
                    }
                },
            )
            composable<Route.Home> {
                val viewModel: HomeTabViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                HomeTabScreen(
                    uiState = uiState,
                    onRecipientChipClick = {
                        // TODO: 수신인 지정 화면 Route 추가 후 연결
                    },
                    onAnswerClick = {
                        // TODO: 데일리 질문 답변 화면 Route 추가 후 연결
                    },
                    onNextStepClick = {
                        appState.navigateToBottomBarRoute(Route.Afternote)
                    },
                    onRecordCategoryClick = { category ->
                        appState.navController.navigate(Route.MindRecord)
                    },
                    onMemoriesSectionClick = {
                        appState.navController.navigate(Route.MemorySpace)
                    },
                    onSettingClick = {
                        appState.navController.navigate(Route.Setting)
                    },
                )
            }
            composable<Route.Setting> {
                SettingScreen(
                    onLogoutSuccess = {
                        appState.navController.navigate(Route.Onboarding) {
                            popUpTo(appState.navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            mindRecordNavGraph(
                onMemorySpaceBack = { appState.navController.popBackStack() },
            )
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
