package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.MainViewModel
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.ui.Route

/**
 * Activity 가 큐에 넣은 알림 진입을 앱 셸이 소비해 목적지 탭으로 옮긴다.
 *
 * 로그인이 확정되고 앱 셸이 바텀바 스택 위에 있을 때만 소비한다. 로그아웃(온보딩 FLOW 스택)이면
 * 큐에 남겨 두어 알림이 인증 관문을 건너뛰는 통로가 되지 않게 하고, 로그인이 끝나 스택이
 * 바뀌는 순간 효과가 다시 돌아 그 목적지로 이어진다. 이동은 바텀바 탭을 누른 것과 같은
 * [AppState.navigateToBottomBarRoute] 를 재사용해 백스택 규약을 한 벌로 유지한다.
 */
@Composable
internal fun NotificationEntryEffect(
    appState: AppState,
    mainViewModel: MainViewModel,
) {
    val pendingEntry by mainViewModel.pendingNotificationEntry.collectAsStateWithLifecycle()
    val authenticatedStartRoute by mainViewModel.startRoute.collectAsStateWithLifecycle()
    val isReadyForDestination =
        authenticatedStartRoute == Route.Home && appState.activeStack != AppStack.FLOW
    val entry = pendingEntry
    LaunchedEffect(entry?.identityKey, isReadyForDestination) {
        if (entry == null || !isReadyForDestination) return@LaunchedEffect
        appState.navigateToBottomBarRoute(entry.destination.toBottomBarRoute())
        mainViewModel.consumeNotificationEntry(entry.identityKey)
    }
}

internal fun NotificationDestination.toBottomBarRoute(): Route =
    when (this) {
        NotificationDestination.HOME -> Route.Home
        NotificationDestination.MIND_RECORD -> Route.MindRecord
        NotificationDestination.TIME_LETTER -> Route.TimeLetter
        NotificationDestination.AFTERNOTE -> Route.Afternote
    }
