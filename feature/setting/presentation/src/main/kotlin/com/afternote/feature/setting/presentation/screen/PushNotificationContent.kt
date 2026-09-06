package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.PushToggleSection
import com.afternote.feature.setting.presentation.component.SettingLoadErrorContent
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState

/**
 * 푸시 알림 설정 화면의 상태 없는 본문.
 *
 * 기기 알림 on/off·SMS/이메일/푸시 채널 동의는 [NotificationSettingScreen] 이 들고 있다 (#560) —
 * 이 화면은 기기 알림이 켜진 뒤에만 진입하는 카테고리별(뉴스레터·마음의 기록·애프터노트) 토글만 그린다.
 * ViewModel 은 [PushNotificationScreen] 이 들고, 이 함수는 넘겨받은 상태만 그린다.
 */
@Composable
internal fun PushNotificationContent(
    uiState: PushNotificationUiState,
    onBack: () -> Unit,
    onNewsletterToggle: (Boolean) -> Unit,
    onMindRecordToggle: (Boolean) -> Unit,
    onAfternoteToggle: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.push_notification_title),
                onBackClick = onBack,
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    uiState.errorMessage != null -> {
                        SettingLoadErrorContent(
                            message = uiState.errorMessage.asString(),
                            onRetry = onRetry,
                        )
                    }

                    else -> {
                        PushToggleSection(
                            uiState = uiState,
                            onNewsletterToggle = onNewsletterToggle,
                            onMindRecordToggle = onMindRecordToggle,
                            onAfternoteToggle = onAfternoteToggle,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.push_notification_device_guide),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PushNotificationContentPreview() {
    PushNotificationContent(
        uiState = PushNotificationUiState(isLoading = false, isAfternoteOn = true),
        onBack = {},
        onNewsletterToggle = {},
        onMindRecordToggle = {},
        onAfternoteToggle = {},
        onRetry = {},
    )
}
