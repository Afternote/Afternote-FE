package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.afternote.core.common.deeplink.NavigationTarget
import com.afternote.core.ui.Route

/**
 * 관문을 통과한 링크 목적지를 루트 그래프에서 **정확히 한 번** 재개한다 (#924).
 *
 * [resumableTarget] 은 이미 로그인 관문을 통과한 값만 온다([com.afternote.afternote_fe.MainViewModel]).
 * 여기서 더 보는 것은 루트 그래프가 재개를 받을 상태인지 하나다.
 *
 * ## 왜 온보딩을 지나기까지 기다리나
 *
 * 로그인 성공은 두 가지를 연달아 일으킨다 — 세션 저장으로 `isLoggedIn` 이 true 가 되고, 그
 * 다음에 온보딩 스택이 `popUpTo(0) { inclusive }` 로 루트를 비우고 홈을 놓는다. 그 사이에
 * 목적지를 쌓으면 바로 뒤따르는 그 교체가 방금 쌓은 화면까지 함께 걷어내, 사용자는 링크를
 * 눌렀는데 홈에 서 있게 된다. 그래서 「로그인됨」이 아니라 **루트가 온보딩을 떠났음**을 본다.
 *
 * 현재 엔트리가 없는 동안(`NavHost` 가 아직 그래프를 세우기 전)도 같은 조건에 걸려 기다린다.
 */
@Composable
internal fun AppLinkResumeEffect(
    appState: AppState,
    resumableTarget: NavigationTarget?,
    onTargetResumed: (NavigationTarget) -> Unit,
) {
    val currentEntry by appState.navController.currentBackStackEntryAsState()
    val isPastOnboarding =
        currentEntry?.destination?.hasRoute(Route.Onboarding::class) == false
    val onResumed by rememberUpdatedState(onTargetResumed)

    LaunchedEffect(appState, resumableTarget, isPastOnboarding) {
        val target = resumableTarget ?: return@LaunchedEffect
        if (!isPastOnboarding) return@LaunchedEffect

        appState.navigateToAppLinkTarget(target)
        onResumed(target)
    }
}
