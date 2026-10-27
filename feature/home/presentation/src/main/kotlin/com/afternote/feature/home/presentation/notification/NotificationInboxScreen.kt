package com.afternote.feature.home.presentation.notification

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.repository.notification.NotificationInboxItem
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.home.presentation.R

/**
 * 서버 알림함 화면 (#2321).
 *
 * 항목을 누르면 [onItemClick] 으로 알림 ID 와 목적지 data 를 넘긴다. 목적지 확인·읽음 처리·이동은
 * 푸시 알림을 누른 것과 같은 앱 셸 경로가 맡는다 — 같은 알림이 들어온 길에 따라 다른 화면으로 가지 않게.
 */
@Composable
fun NotificationInboxScreen(
    onBack: () -> Unit,
    onItemClick: (notificationId: Long, data: Map<String, String>) -> Unit,
) {
    val viewModel: NotificationInboxViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) isFirstResume = false else viewModel.onIntent(NotificationInboxIntent.Refresh)
    }
    NotificationInboxContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = { viewModel.onIntent(NotificationInboxIntent.Refresh) },
        onItemClick = { item ->
            viewModel.onIntent(NotificationInboxIntent.Open(item.id))
            onItemClick(item.id, item.data)
        },
    )
}

/**
 * 홈 탑바 알림 점. 화면이 보일 때마다(처음 포함) 다시 센다 — 알림함에서 읽고 돌아오거나 푸시를 받은 뒤
 * 홈으로 돌아오면 점이 맞춰진다.
 */
@Composable
fun rememberHasUnreadNotification(): Boolean {
    val viewModel: NotificationBadgeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(NotificationBadgeIntent.Refresh) }
    return uiState.hasUnread
}
