package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.DeviceAlarmOffSection
import com.afternote.feature.setting.presentation.component.PushToggleSection
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState

/**
 * 푸시 알림 설정 화면의 상태 없는 본문.
 *
 * ViewModel·시스템 설정 이동은 [PushNotificationScreen] 이 들고, 이 함수는 넘겨받은 상태만 그린다.
 */
@Composable
internal fun PushNotificationContent(
    uiState: PushNotificationUiState,
    onBack: () -> Unit,
    onDeviceAlarmClick: () -> Unit,
    onNewsletterToggle: (Boolean) -> Unit,
    onMindRecordToggle: (Boolean) -> Unit,
    onAfternoteToggle: (Boolean) -> Unit,
    onSmsCheck: (Boolean) -> Unit,
    onEmailCheck: (Boolean) -> Unit,
    onPushCheck: (Boolean) -> Unit,
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
            // 기기 알림 설정 행 — 항상 표시, 탭하면 Android 시스템 알림 설정으로 이동
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceAlarmClick() }
                        .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.device_alarm_setting),
                    style = AfternoteDesign.typography.bodyLargeR,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text =
                        if (uiState.isDeviceAlarmOn) {
                            stringResource(R.string.device_alarm_on)
                        } else {
                            stringResource(R.string.device_alarm_off)
                        },
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.isDeviceAlarmOn) {
                PushToggleSection(
                    uiState = uiState,
                    onNewsletterToggle = onNewsletterToggle,
                    onMindRecordToggle = onMindRecordToggle,
                    onAfternoteToggle = onAfternoteToggle,
                )
            } else {
                DeviceAlarmOffSection(
                    uiState = uiState,
                    onSmsCheck = onSmsCheck,
                    onEmailCheck = onEmailCheck,
                    onPushCheck = onPushCheck,
                )
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

@Preview(name = "기기 알림 켜짐", showBackground = true)
@Composable
private fun PreviewAlarmOn() {
    PushNotificationContent(
        uiState = PushNotificationUiState(isDeviceAlarmOn = true),
        onBack = {},
        onDeviceAlarmClick = {},
        onNewsletterToggle = {},
        onMindRecordToggle = {},
        onAfternoteToggle = {},
        onSmsCheck = {},
        onEmailCheck = {},
        onPushCheck = {},
    )
}

@Preview(name = "기기 알림 꺼짐", showBackground = true)
@Composable
private fun PreviewAlarmOff() {
    PushNotificationContent(
        uiState = PushNotificationUiState(isDeviceAlarmOn = false),
        onBack = {},
        onDeviceAlarmClick = {},
        onNewsletterToggle = {},
        onMindRecordToggle = {},
        onAfternoteToggle = {},
        onSmsCheck = {},
        onEmailCheck = {},
        onPushCheck = {},
    )
}
