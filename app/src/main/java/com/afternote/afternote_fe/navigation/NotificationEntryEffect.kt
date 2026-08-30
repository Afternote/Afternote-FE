package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.MainViewModel
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.ui.Route

/**
 * 알림 탭으로 도착한 진입 이벤트를 실제 화면 이동으로 옮긴다 (#1111).
 *
 * `MainActivity`의 `onCreate`/`onNewIntent`가 큐에 넣은 [MainViewModel.pendingNotificationEntry]
 * 를 여기서 소비한다. cold·background·foreground 세 경로가 모두 그 큐 하나로 합류하므로 이동
 * 규칙도 이 한 곳에만 있다.
 *
 * `MainActivity`와 같은 `ViewModelStoreOwner`(Activity)에서 [hiltViewModel]을 받으므로 Activity가
 * `by viewModels()`로 쥔 것과 **같은 인스턴스**다. 파라미터로 내려보내지 않는 이유는
 * `NotificationPermissionEffect`와 같다 — 배선을 빠뜨려도 컴파일이 통과하는 no-op 디폴트 콜백을
 * 만들지 않기 위해서다.
 *
 * @param isSignedIn 로그인 상태. `false`면 이동하지 않고 큐에 남겨 둔다.
 */
@Composable
internal fun NotificationEntryEffect(
    appState: AppState,
    isSignedIn: Boolean,
) {
    val viewModel: MainViewModel = hiltViewModel()
    val entry by viewModel.pendingNotificationEntry.collectAsStateWithLifecycle()
    val identityKey = entry?.identityKey

    LaunchedEffect(identityKey, isSignedIn) {
        val request = entry ?: return@LaunchedEffect
        // 로그아웃 상태에서 이동하면 알림이 온보딩·인증 관문을 건너뛰는 통로가 된다. 소비하지
        // 않고 두므로 인증이 확정되며 startDestination이 홈으로 바뀌는 순간 이 효과가 다시 돈다.
        if (!isSignedIn) return@LaunchedEffect

        appState.navigateToBottomBarRoute(request.destination.toRoute())
        viewModel.consumeNotificationEntry(request.identityKey)
    }
}

/**
 * 알림 목적지 계약값을 앱 최상위 [Route]로 옮긴다.
 *
 * `when`이 exhaustive라 [NotificationDestination]에 값을 더하면 여기가 컴파일로 막는다 —
 * 「보내는 쪽에만 추가하고 도착지를 안 만든 목적지」가 생기지 않는다.
 */
internal fun NotificationDestination.toRoute(): Route =
    when (this) {
        NotificationDestination.HOME -> Route.Home
        NotificationDestination.MIND_RECORD -> Route.MindRecord
        NotificationDestination.TIME_LETTER -> Route.TimeLetter
        NotificationDestination.AFTERNOTE -> Route.Afternote
    }
