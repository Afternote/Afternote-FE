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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.afternote.afternote_fe.notification.NotificationPermissionEffect
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.navigation.afternoteNavGraph
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.receiver.navigation.receivedAfternoteNavGraph
import com.afternote.feature.home.presentation.HomeTabScreen
import com.afternote.feature.home.presentation.HomeTabViewModel
import com.afternote.feature.home.presentation.receiver.ReceiverHomeEntry
import com.afternote.feature.mindrecord.presentation.navigation.mindRecordNavGraph
import com.afternote.feature.onboarding.presentation.navigation.onboardingNavGraph
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.receiver.presentation.navigation.receiverNavGraph
import com.afternote.feature.setting.presentation.navigation.settingNavGraph
import com.afternote.feature.timeletter.presentation.navigation.timeLetterNavGraph
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

    val onboardingNavActions = rememberOnboardingNavActions(appState.navController)
    val mindRecordNavActions = rememberMindRecordNavActions(appState.navController)
    val settingNavActions = rememberSettingNavActions(appState)
    val timeLetterNavActions = rememberTimeLetterNavActions(appState.navController)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val afternoteNavActions =
        rememberAfternoteNavActions(appState) { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    val receivedAfternoteNavActions = rememberReceivedAfternoteNavActions(appState)
    val receiverNavActions = rememberReceiverNavActions(appState)
    val receiverHomeActions = rememberReceiverHomeActions(appState)

    // 13+ 는 런타임 권한이 없으면 알림이 한 건도 게시되지 않는다 (#1454).
    NotificationPermissionEffect(snackbarHostState = snackbarHostState)

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

                // 다른 화면(일기·데일리질문 작성, 수신인 지정 등)에서 홈으로 복귀 시 홈 요약 refetch.
                // 최초 진입은 VM 의 init { loadHomeSummary() } 가 이미 로드하므로 첫 resume 은 스킵한다.
                // (rememberSaveable: 다른 화면 이동으로 컴포지션에서 벗어나도 스킵 플래그가 초기화되지 않도록)
                var isFirstResume by rememberSaveable { mutableStateOf(true) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    if (isFirstResume) {
                        isFirstResume = false
                    } else {
                        viewModel.refreshOnReturn()
                    }
                }

                HomeTabScreen(
                    uiState = uiState,
                    actions = homeTabActions,
                )
            }
            settingNavGraph(
                graphScopedParentEntry = {
                    appState.navController.getBackStackEntry<Route.Setting>()
                },
                actions = settingNavActions,
            )
            mindRecordNavGraph(actions = mindRecordNavActions)
            timeLetterNavGraph(
                navController = appState.navController,
                actions = timeLetterNavActions,
            )
            afternoteNavGraph(
                graphScopedParentEntry = {
                    appState.navController.getBackStackEntry<Route.Afternote>()
                },
                editorFlowParentEntry = {
                    appState.navController.getBackStackEntry<AfternoteRoute.EditorFlowRoute>()
                },
                actions = afternoteNavActions,
            )
            // 수신 애프터노트 화면은 애프터노트 피처가 갖는다 (#1461). Route.Afternote 그래프는
            // 발신자용 지문 관문을 시작점으로 삼으므로 그 안에 중첩하지 않고 루트에 직접 등록한다.
            receivedAfternoteNavGraph(actions = receivedAfternoteNavActions)
            receiverNavGraph(
                homeContent = { ReceiverHomeEntry(actions = receiverHomeActions) },
                actions = receiverNavActions,
                // 열람 신청 nested graph 의 parent route. 이 route 의 backStackEntry 가 자체 ViewModelStore 를
                // 보유 → 자식 5 화면이 그 안의 DeliveryVerificationFlowViewModel 을 공유 (flow-scoped VM).
                deliveryFlowParentEntry = {
                    appState.navController.getBackStackEntry<ReceiverRoute.DeliveryVerificationFlowRoute>()
                },
            )
        }
    }
}
