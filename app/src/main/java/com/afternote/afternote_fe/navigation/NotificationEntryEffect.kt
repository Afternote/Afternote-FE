package com.afternote.afternote_fe.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.MainViewModel
import com.afternote.afternote_fe.R
import com.afternote.afternote_fe.notification.NotificationNavigation
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.ui.Route
import kotlinx.coroutines.launch

/**
 * Activity 가 큐에 넣은 알림 진입을 앱 셸이 소비해 목적지로 옮긴다.
 *
 * 로그인이 확정되고 앱 셸이 바텀바 스택 위에 있을 때만 소비한다. 로그아웃(온보딩 FLOW 스택)이면
 * 큐에 남겨 두어 알림이 인증 관문을 건너뛰는 통로가 되지 않게 하고, 로그인이 끝나 스택이
 * 바뀌는 순간 효과가 다시 돌아 그 목적지로 이어진다.
 *
 * 이동은 바텀바 탭을 누르거나 홈 카드를 누른 것과 같은 [AppState] 경로를 재사용해 백스택 규약을 한 벌로
 * 유지한다. 서버가 요구하는 「뒤로가기는 홈」 도 그 규약이 지킨다 — 탭 안쪽 화면은 탭 홈 위에, 설정은 홈
 * 위에 쌓이고, 탭 홈에서의 back 은 홈으로 간다.
 *
 * 서버 알림이면 이동 전에 현재 목적지를 서버에 묻는다(#2321). 대상이 지워졌으면 그 목록으로 가서
 * 안내를 띄운다. 안내는 [rememberCoroutineScope] 로 띄운다 — 소비하는 순간 이 효과의 키가 바뀌어
 * 효과 안에서 띄운 스낵바는 바로 취소된다.
 */
@Composable
internal fun NotificationEntryEffect(
    appState: AppState,
    mainViewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val pendingEntry by mainViewModel.pendingNotificationEntry.collectAsStateWithLifecycle()
    val authenticatedStartRoute by mainViewModel.startRoute.collectAsStateWithLifecycle()
    val isReadyForDestination =
        authenticatedStartRoute == Route.Home && appState.activeStack != AppStack.FLOW
    val noticeScope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.notification_target_deleted_message)
    val entry = pendingEntry
    LaunchedEffect(entry?.identityKey, isReadyForDestination) {
        if (entry == null || !isReadyForDestination) return@LaunchedEffect
        val resolution = mainViewModel.resolveNotificationEntry(entry)
        appState.navigateTo(resolution.navigation)
        mainViewModel.consumeNotificationEntry(entry.identityKey)
        if (resolution.deleted) {
            noticeScope.launch {
                snackbarHostState.showSnackbar(resolution.serverMessage ?: deletedMessage)
            }
        }
    }
}

private fun AppState.navigateTo(navigation: NotificationNavigation) {
    when (navigation) {
        is NotificationNavigation.Tab -> {
            navigateToBottomBarRoute(navigation.tab.toBottomBarRoute())
        }

        is NotificationNavigation.MindRecord -> {
            navigateToMindRecordDestination(navigation.route)
        }

        is NotificationNavigation.TimeLetter -> {
            navigateToTimeLetterDestinations(navigation.routes)
        }

        is NotificationNavigation.Setting -> {
            navigateToBottomBarRoute(Route.Home)
            navigate(navigation.route)
        }
    }
}

private fun NotificationDestination.toBottomBarRoute(): Route =
    when (this) {
        NotificationDestination.HOME -> Route.Home
        NotificationDestination.MIND_RECORD -> Route.MindRecord
        NotificationDestination.TIME_LETTER -> Route.TimeLetter
        NotificationDestination.AFTERNOTE -> Route.Afternote
    }
