package com.afternote.afternote_fe.navigation

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.home.presentation.HomeTabActions
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.onboarding.presentation.navigation.OnboardingExternalActions
import com.afternote.feature.setting.presentation.navigation.SettingNavActions
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterNavActions
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute

/**
 * Nav2 백스택 회귀 기준(#1601)이 공유하는 하네스.
 *
 * 검사 대상은 **앱 모듈의 네비게이션 명령**(`AppState.navigateToBottomBarRoute` 와
 * `AppNavigationActions.kt` 의 `remember*NavActions`)이 만드는 백스택의 **모양과 수명**이다.
 * 그래서 목적지는 실제 화면 대신 라벨만 그리는 stub 으로 두되,
 * **라우트 심볼과 그래프 중첩 구조는 프로덕션 그래프를 그대로 옮긴다** — `popUpTo<T>` 와
 * `saveState`/`restoreState` 의 결과가 정확히 그 토폴로지에 달려 있기 때문이다.
 *
 * 즉 이 하네스는 화면 구현이 아니라 **경로 배치**를 고정한다. 프로덕션 그래프가 라우트를
 * 옮기거나 중첩을 바꾸면 [appRouteSkeleton] 도 같이 고쳐야 한다.
 */
internal class NavBackStackHarness(
    val appState: AppState,
    val onboardingExternalActions: OnboardingExternalActions,
    val settingActions: SettingNavActions,
    val timeLetterActions: TimeLetterNavActions,
    val homeActions: HomeTabActions,
    /** predictive back 제스처를 프로그램으로 흘려 넣기 위한 호스트 dispatcher. */
    val backDispatcher: OnBackPressedDispatcher,
) {
    val navController: NavHostController get() = appState.navController
}

/**
 * 프로덕션 `AppNavigation` 의 NavHost 를 stub 목적지로 재현한다.
 *
 * @param onReady 컴포지션이 만든 실제 [AppState] 와 실제 nav actions 를 테스트로 넘긴다.
 */
@Composable
internal fun SkeletonAppNavigation(
    startDestination: Route,
    onReady: (NavBackStackHarness) -> Unit,
) {
    val appState = rememberAfternoteAppState()
    val onboardingExternalActions = rememberOnboardingExternalActions(appState)
    val settingActions = rememberSettingNavActions(appState)
    val timeLetterActions = rememberTimeLetterNavActions(appState.navController)
    val homeActions = rememberHomeTabActions(appState, onRetryLoad = { })
    val backDispatcher =
        checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
            "테스트 호스트가 OnBackPressedDispatcherOwner 를 제공하지 않는다"
        }.onBackPressedDispatcher

    SideEffect {
        onReady(
            NavBackStackHarness(
                appState = appState,
                onboardingExternalActions = onboardingExternalActions,
                settingActions = settingActions,
                timeLetterActions = timeLetterActions,
                homeActions = homeActions,
                backDispatcher = backDispatcher,
            ),
        )
    }

    NavHost(
        navController = appState.navController,
        startDestination = startDestination,
    ) {
        appRouteSkeleton()
    }
}

/**
 * `AppNavigation` 이 등록하는 그래프들의 **라우트 토폴로지**만 옮긴 뼈대.
 *
 * **중첩과 시작점(`navigation<...>(startDestination = ...)`)은 프로덕션 그래프와 정확히 같다** —
 * `popUpTo<T>` 와 `saveState`/`restoreState` 의 결과가 거기에 달려 있기 때문이다. 반면 개별
 * `composable<...>` 은 테스트가 실제로 지나는 목적지만 옮긴다(설정·마음의 기록·타임레터는
 * 부분집합이고, 어느 화면을 왜 뒀는지는 각 구역 주석에 적었다). 백스택 «모양» 판정에는 지나지
 * 않는 형제 목적지가 영향을 주지 않는다.
 *
 * 화면 본문은 라벨 하나뿐이고, 일부 목적지는 `rememberSaveable` 카운터를 들고 있어
 * «탭을 떠났다 돌아왔을 때 화면 상태가 살아 있는가» 를 잴 수 있다.
 */
@Suppress("LongMethod")
internal fun NavGraphBuilder.appRouteSkeleton() {
    // ── Navigation 3 로컬 스택을 가진 그래프 (#1698) — 루트엔 host destination 하나씩이다.
    // 그래프 «안쪽» 백스택 모양은 각 피처의 *LocalNavActions 테스트가 본다. 여기서 재현할 것은
    // 루트가 보는 모양뿐이다.
    stubScreen<Route.Onboarding>()
    // 애프터노트만 stateful — 탭 재진입에서 저장된 상태가 복원되지 «않는» 것이 판정 대상이다.
    stubScreen<Route.Afternote>(stateful = true)
    stubScreen<Route.ReceivedAfternote>()
    stubScreen<Route.Receiver>()

    // app — AppNavigation.kt 가 직접 등록하는 홈 탭
    stubScreen<Route.Home>(stateful = true)

    // feature/setting — SettingNavGraph.kt. 로그아웃·탈퇴 경계가 지나는 화면만 옮긴다.
    navigation<Route.Setting>(startDestination = SettingRoute.SettingHomeRoute) {
        stubScreen<SettingRoute.SettingHomeRoute>()
        stubScreen<SettingRoute.WithdrawGuideRoute>()
        stubScreen<SettingRoute.WithdrawConfirmRoute>()
    }

    // feature/mindrecord — MindRecordNavGraph.kt. 중첩 없이 루트에 직접 붙는 top-level 3종과
    // 임시저장 목록·기록 상세만 옮긴다(작성 화면은 어느 테스트도 지나지 않는다).
    // 기록 상세는 홈 MEMORIES 의 「그날의 기록 다시 읽기」가 인자까지 싣고 도착하는 목적지다 (#793).
    stubScreen<Route.MindRecord>()
    stubScreen<Route.MemorySpace>()
    stubScreen<Route.ReceiverMindRecord>()
    stubScreen<MindRecordRoute.DraftListRoute>()
    stubScreen<MindRecordRoute.RecordDetailRoute>()

    // feature/timeletter — TimeLetterNavGraph.kt. 탭 저장/복원과 predictive back 이 지나는
    // 세 화면만 옮긴다.
    navigation<Route.TimeLetter>(startDestination = TimeLetterRoute.TimeLetterHomeRoute) {
        stubScreen<TimeLetterRoute.TimeLetterHomeRoute>()
        stubScreen<TimeLetterRoute.TimeLetterDraftRoute>(stateful = true)
        stubScreen<TimeLetterRoute.TimeLetterRecipientRoute>()
    }
}

/**
 * 라우트 이름만 그리는 stub 목적지.
 *
 * @param stateful `true` 면 `rememberSaveable` 카운터를 함께 그린다. 화면을 눌러 올린 값이
 * 탭 전환·프로세스 재생성 뒤에도 남는지가 곧 «화면 상태 저장/복원» 의 판정이다.
 */
private inline fun <reified T : Any> NavGraphBuilder.stubScreen(stateful: Boolean = false) {
    composable<T> { entry ->
        val label = entry.destination.simpleRouteName()
        if (stateful) {
            var taps by rememberSaveable { mutableIntStateOf(0) }
            BasicText(
                text = "$label#$taps",
                modifier = Modifier.clickable { taps++ },
            )
        } else {
            BasicText(text = label)
        }
    }
}

/**
 * 직렬화 라우트 문자열(`com.afternote...Route.Home/{arg}?q={q}`)에서 심볼 이름만 남긴다.
 * 백스택 «모양» 을 사람이 읽는 단위로 단언하기 위한 것이라, 인자 값은 일부러 버린다.
 */
internal fun NavDestination.simpleRouteName(): String =
    route
        ?.substringBefore('/')
        ?.substringBefore('?')
        ?.substringAfterLast('.')
        ?: "NavHostRoot"

/** 루트 그래프 엔트리를 포함한 현재 백스택의 라우트 이름 목록. */
internal fun NavHostController.backStackRouteNames(): List<String> = currentBackStack.value.map { it.destination.simpleRouteName() }

/** 현재 목적지의 라우트 이름. */
internal fun NavHostController.currentRouteName(): String = currentBackStackEntry?.destination?.simpleRouteName() ?: "<없음>"
