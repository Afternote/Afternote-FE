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
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.navigation.AfternoteNavHost
import com.afternote.feature.afternote.presentation.receiver.navigation.ReceivedAfternoteNavHost
import com.afternote.feature.home.presentation.HomeTabScreen
import com.afternote.feature.home.presentation.HomeTabViewModel
import com.afternote.feature.home.presentation.receiver.ReceiverHomeEntry
import com.afternote.feature.mindrecord.presentation.navigation.mindRecordNavGraph
import com.afternote.feature.onboarding.presentation.navigation.OnboardingNavHost
import com.afternote.feature.receiver.presentation.navigation.ReceiverNavHost
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

    // 로컬 Nav3 스택의 깊이는 Nav2 destination 에 안 보인다 — 애프터노트 host 가 올려 주는 신호를
    // 바텀바 판정에 합성한다. 피처를 떠나면 host 가 true 로 되돌려 다른 탭 판정을 오염시키지 않는다.
    var isAfternoteStackAtRoot by remember { mutableStateOf(true) }

    val showBottomBar = appState.shouldShowBottomBar(currentDestination, isAfternoteStackAtRoot)
    val currentTab = appState.getCurrentNavTab(currentDestination)

    val mindRecordNavActions = rememberMindRecordNavActions(appState.navController)
    val settingNavActions = rememberSettingNavActions(appState)
    val timeLetterNavActions = rememberTimeLetterNavActions(appState.navController)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onboardingExternalActions = rememberOnboardingExternalActions(appState)
    val afternoteExternalActions =
        rememberAfternoteExternalActions(appState) { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    val receiverHomeActions = rememberReceiverHomeActions(appState)

    // 로컬 스택 바닥에서의 back 은 루트 백스택 pop 으로 돌려준다. 루트가 NavDisplay 로 바뀌어도
    // 계약은 그대로고 이 구현만 갈린다 (#1702).
    // 바텀바가 없는 그래프는 깊이를 셸에 올릴 일이 없다.
    val popRootBoundary = rememberRootPopBoundary(appState, onAtRootChanged = {})
    val afternoteBoundary =
        rememberRootPopBoundary(appState) { isAtRoot -> isAfternoteStackAtRoot = isAtRoot }

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
            // ── Navigation 3 로컬 스택을 가진 그래프 (#1698) — 루트엔 host destination 하나씩만 둔다.
            composable<Route.Onboarding> {
                OnboardingNavHost(
                    boundary = popRootBoundary,
                    externalActions = onboardingExternalActions,
                )
            }
            composable<Route.Afternote> {
                AfternoteNavHost(
                    boundary = afternoteBoundary,
                    externalActions = afternoteExternalActions,
                )
            }
            // 수신 애프터노트 화면은 애프터노트 피처가 갖는다 (#1461). Route.Afternote 그래프는
            // 발신자용 지문 관문을 시작점으로 삼으므로 그 안에 중첩하지 않고 루트에 직접 등록한다.
            composable<Route.ReceivedAfternote> {
                ReceivedAfternoteNavHost(boundary = popRootBoundary)
            }
            composable<Route.Receiver> {
                ReceiverNavHost(
                    homeContent = { ReceiverHomeEntry(actions = receiverHomeActions) },
                    boundary = popRootBoundary,
                )
            }

            // ── 아직 Navigation 2 인 그래프 — #1695 · #1696 · #1697 이 각각 이관한다.
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
        }
    }
}

/**
 * 로컬 스택 바닥의 back 을 루트 백스택 pop 으로 돌려주는 경계.
 *
 * @param onAtRootChanged 바텀바 판정에 깊이를 합성해야 하는 그래프만 넘긴다.
 */
@Composable
private fun rememberRootPopBoundary(
    appState: AppState,
    onAtRootChanged: (Boolean) -> Unit,
): FeatureStackBoundary =
    remember(appState, onAtRootChanged) {
        object : FeatureStackBoundary {
            override fun exit() {
                appState.navController.popBackStack()
            }

            override fun onAtRootChanged(isAtRoot: Boolean) = onAtRootChanged(isAtRoot)
        }
    }
