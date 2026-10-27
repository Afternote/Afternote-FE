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

/** 알림함 화면의 상태 없는 본문 — Figma 「알람창」 (#2321). ViewModel 은 [NotificationInboxScreen] 이 든다. */
@Composable
internal fun NotificationInboxContent(
    uiState: NotificationInboxUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (NotificationInboxItem) -> Unit,
) {
    Scaffold(
        topBar = { DetailTopBar(title = stringResource(R.string.home_notification_inbox_title), onBackClick = onBack) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = AfternoteDesign.colors.gray9,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.loadFailed -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.home_notification_inbox_load_failed),
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray7,
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = onRetry) {
                            Text(
                                text = stringResource(R.string.home_tab_retry),
                                style = AfternoteDesign.typography.captionLargeB,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    }
                }

                uiState.items.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.home_notification_inbox_empty),
                        style = AfternoteDesign.typography.bodyBase,
                        color = AfternoteDesign.colors.gray5,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                        items(uiState.items, key = NotificationInboxItem::id) { item ->
                            NotificationInboxRow(item = item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }
    }
}

/**
 * 알림함 한 줄 — Figma 「알람창」 의 alarm_1~4·alarm_check (#2321).
 *
 * 읽은 알림은 아이콘·제목·본문을 모두 gray5 로 낮춘다(alarm_check). 날짜 줄은 서버 알림함 응답에 시각이
 * 없어 아직 그리지 않는다.
 */
@Composable
private fun NotificationInboxRow(
    item: NotificationInboxItem,
    onClick: () -> Unit,
) {
    val readColor = AfternoteDesign.colors.gray5
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .bottomBorder(color = AfternoteDesign.colors.gray3, width = 1.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.iconRes()),
            contentDescription = null,
            colorFilter = if (item.isRead) ColorFilter.tint(readColor) else null,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = AfternoteDesign.typography.captionLargeR,
                color = if (item.isRead) readColor else AfternoteDesign.colors.gray9,
            )
            Text(
                text = item.body,
                style = AfternoteDesign.typography.captionLargeR,
                color = if (item.isRead) readColor else AfternoteDesign.colors.gray6,
            )
        }
    }
}

/** 알림이 가리키는 곳으로 아이콘을 고른다. 기록·질문은 책, 편지·수신인은 편지, 설정을 권하면 연필, 확인이 필요하면 느낌표. */
@DrawableRes
private fun NotificationInboxItem.iconRes(): Int =
    when (data["destination"]) {
        "TIME_LETTER_LIST", "TIME_LETTER_DETAIL", "TIME_LETTER_COMPOSE",
        "RECEIVER_LIST", "RECEIVER_DETAIL", "INVITATION_DETAIL", "INVITATION", "RECEIVER_HOME",
        -> R.drawable.home_ic_notification_letter

        "AGENT_SETTINGS" -> R.drawable.home_ic_notification_setting

        "RETENTION_CONFIRMATION", "AGENT_CONFIRMATION" -> R.drawable.home_ic_notification_alert

        else -> R.drawable.home_ic_notification_record
    }
