package com.afternote.afternote_fe.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.entryProvider
import com.afternote.afternote_fe.notification.NotificationPermissionEffect
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureNavigationCallbacks
import com.afternote.core.ui.navigation.NavDestinationBackground
import com.afternote.core.ui.navigation.PredictiveBackPopEnter
import com.afternote.core.ui.navigation.PredictiveBackPopExit
import com.afternote.feature.afternote.presentation.navigation.AfternoteNavHost
import com.afternote.feature.afternote.presentation.receiver.navigation.ReceivedAfternoteNavHost
import com.afternote.feature.home.presentation.HomeTabScreen
import com.afternote.feature.home.presentation.HomeTabViewModel
import com.afternote.feature.home.presentation.receiver.ReceiverHomeEntry
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordNavActions
import com.afternote.feature.mindrecord.presentation.navigation.mindRecordNavGraph
import com.afternote.feature.onboarding.presentation.navigation.OnboardingNavHost
import com.afternote.feature.receiver.presentation.navigation.ReceiverNavHost
import com.afternote.feature.setting.presentation.navigation.SettingNavHost
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
import com.afternote.feature.timeletter.presentation.navigation.timeLetterNavGraph
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    startDestination: Route,
    modifier: Modifier = Modifier,
    appState: AppState = rememberAfternoteAppState(startDestination),
    mindRecordNavController: NavHostController? = null,
    timeLetterNavController: NavHostController? = null,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val rememberedMindRecordController = rememberNavController()
    val rememberedTimeLetterController = rememberNavController()
    val mindRecordController = mindRecordNavController ?: appState.legacyMindRecordController ?: rememberedMindRecordController
    val timeLetterController = timeLetterNavController ?: rememberedTimeLetterController
    val mindRecordEntry by mindRecordController.currentBackStackEntryAsState()
    val timeLetterEntry by timeLetterController.currentBackStackEntryAsState()
    val isMindRecordAtRoot = mindRecordEntry?.destination?.hasRoute(Route.MindRecord::class) != false
    val isTimeLetterAtRoot =
        timeLetterEntry?.destination?.hasRoute(TimeLetterRoute.TimeLetterHomeRoute::class) != false
    var isAfternoteStackAtRoot by remember { mutableStateOf(true) }
    val showBottomBar = appState.shouldShowBottomBar(isMindRecordAtRoot, isTimeLetterAtRoot, isAfternoteStackAtRoot)
    val currentTab = appState.getCurrentNavTab()

    val mindRecordNavActions = rememberMindRecordNavActions(mindRecordController)
    val timeLetterNavActions =
        rememberTimeLetterNavActions(
            navController = timeLetterController,
            onNavigateToSetting = { appState.navigate(Route.Setting()) },
        )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onboardingExternalActions = rememberOnboardingExternalActions(appState)
    val settingExternalActions = rememberSettingExternalActions(appState)
    val afternoteExternalActions =
        rememberAfternoteExternalActions(appState) { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    val receiverHomeActions = rememberReceiverHomeActions(appState)
    val rootNavigationCallbacks =
        rememberRootNavigationCallbacks(
            appState = appState,
            onAppRootExit = { activity?.finish() },
            onAtRootChanged = {},
        )
    val afternoteNavigationCallbacks =
        rememberRootNavigationCallbacks(
            appState = appState,
            onAppRootExit = { activity?.finish() },
            onAtRootChanged = { isAtRoot -> isAfternoteStackAtRoot = isAtRoot },
        )

    val pendingMindRecordDestination = appState.pendingMindRecordDestination
    SideEffect { appState.initializeLegacyStartDestination(startDestination) }
    LaunchedEffect(appState.activeStack, mindRecordEntry, timeLetterEntry) {
        if (appState.activeStack == AppStack.FLOW) {
            if (mindRecordEntry != null) {
                mindRecordController.navigate(Route.MindRecord) {
                    popUpTo<Route.MindRecord> { inclusive = false }
                    launchSingleTop = true
                }
            }
            if (timeLetterEntry != null) {
                timeLetterController.navigate(Route.TimeLetter) {
                    popUpTo<Route.TimeLetter> { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }
    LaunchedEffect(pendingMindRecordDestination, mindRecordEntry) {
        val destination = pendingMindRecordDestination ?: return@LaunchedEffect
        if (mindRecordEntry != null) {
            mindRecordController.navigate(Route.MindRecord) {
                popUpTo<Route.MindRecord> { inclusive = false }
                launchSingleTop = true
            }
            mindRecordController.navigate(destination)
            appState.consumeMindRecordDestination(destination)
        }
    }

    NotificationPermissionEffect(snackbarHostState = snackbarHostState)

    Scaffold(
        modifier = modifier,
        containerColor = NavDestinationBackground,
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
        FeatureNavDisplay(
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            backStack = appState.backStack,
            boundary = rootNavigationCallbacks,
            predictivePopTransitionSpec = { _ ->
                ContentTransform(PredictiveBackPopEnter, PredictiveBackPopExit)
            },
            entryProvider =
                entryProvider {
                    entry<Route.Onboarding> {
                        OnboardingNavHost(
                            boundary = rootNavigationCallbacks,
                            externalActions = onboardingExternalActions,
                        )
                    }
                    entry<Route.Home> {
                        val viewModel: HomeTabViewModel = hiltViewModel()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        val actions =
                            rememberHomeTabActions(
                                appState = appState,
                                onRetryLoad = { viewModel.loadHomeSummary(isRefresh = true) },
                            )
                        var isFirstResume by rememberSaveable { mutableStateOf(true) }
                        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                            if (isFirstResume) isFirstResume = false else viewModel.refreshOnReturn()
                        }
                        HomeTabScreen(uiState = uiState, actions = actions)
                    }
                    entry<Route.MindRecord> {
                        MindRecordRootHost(
                            navController = mindRecordController,
                            actions = mindRecordNavActions,
                        )
                    }
                    entry<Route.TimeLetter> {
                        NavHost(navController = timeLetterController, startDestination = Route.TimeLetter::class) {
                            timeLetterNavGraph(navController = timeLetterController, actions = timeLetterNavActions)
                        }
                    }
                    entry<Route.Afternote> {
                        AfternoteNavHost(
                            navigationCallbacks = afternoteNavigationCallbacks,
                            externalActions = afternoteExternalActions,
                        )
                    }
                    entry<Route.ReceivedAfternote> {
                        ReceivedAfternoteNavHost(navigationCallbacks = rootNavigationCallbacks)
                    }
                    entry<Route.Receiver> {
                        ReceiverNavHost(
                            homeContent = { ReceiverHomeEntry(actions = receiverHomeActions) },
                            navigationCallbacks = rootNavigationCallbacks,
                        )
                    }
                    entry<Route.Setting> { route ->
                        SettingNavHost(
                            navigationCallbacks = rootNavigationCallbacks,
                            externalActions = settingExternalActions,
                            startWithRecipientRegistration = route.startWithRecipientRegistration,
                        )
                    }
                },
        )
    }
}

@Composable
private fun MindRecordRootHost(
    navController: NavHostController,
    actions: MindRecordNavActions,
) {
    NavHost(navController = navController, startDestination = Route.MindRecord::class) {
        mindRecordNavGraph(actions = actions)
    }
}

@Composable
private fun rememberRootNavigationCallbacks(
    appState: AppState,
    onAppRootExit: () -> Unit,
    onAtRootChanged: (Boolean) -> Unit,
): FeatureNavigationCallbacks =
    remember(appState, onAppRootExit, onAtRootChanged) {
        object : FeatureNavigationCallbacks {
            override fun exit() {
                if (!appState.exitCurrentEntry()) onAppRootExit()
            }

            override fun onAtRootChanged(isAtRoot: Boolean) = onAtRootChanged(isAtRoot)
        }
    }

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
