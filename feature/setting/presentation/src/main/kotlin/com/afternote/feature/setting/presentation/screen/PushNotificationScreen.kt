package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.DeviceAlarmOffSection
import com.afternote.feature.setting.presentation.component.PushToggleSection
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel

@Composable
fun PushNotificationScreen(
    onBack: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PushNotificationContent(
        uiState = uiState,
        onBack = onBack,
        onNewsletterToggle = viewModel::onNewsletterToggle,
        onMindRecordToggle = viewModel::onMindRecordToggle,
        onAfternoteToggle = viewModel::onAfternoteToggle,
        onSmsChecked = viewModel::onSmsChecked,
        onEmailChecked = viewModel::onEmailChecked,
        onPushChecked = viewModel::onPushChecked,
    )
}

@Composable
private fun PushNotificationContent(
    uiState: PushNotificationUiState,
    onBack: () -> Unit,
    onNewsletterToggle: (Boolean) -> Unit,
    onMindRecordToggle: (Boolean) -> Unit,
    onAfternoteToggle: (Boolean) -> Unit,
    onSmsChecked: (Boolean) -> Unit,
    onEmailChecked: (Boolean) -> Unit,
    onPushChecked: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.push_notification_title),
                onBackClick = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
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
                    onSmsChecked = onSmsChecked,
                    onEmailChecked = onEmailChecked,
                    onPushChecked = onPushChecked,
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.push_notification_device_guide),
            )
        }
    }
}

@Preview(name = "기기 알림 켜짐")
@Composable
private fun PreviewAlarmOn() {
    PushNotificationContent(
        uiState = PushNotificationUiState(isDeviceAlarmOn = true),
        onBack = {},
        onNewsletterToggle = {},
        onMindRecordToggle = {},
        onAfternoteToggle = {},
        onSmsChecked = {},
        onEmailChecked = {},
        onPushChecked = {},
    )
}

@Preview(name = "기기 알림 꺼짐")
@Composable
private fun PreviewAlarmOff() {
    PushNotificationContent(
        uiState = PushNotificationUiState(isDeviceAlarmOn = false),
        onBack = {},
        onNewsletterToggle = {},
        onMindRecordToggle = {},
        onAfternoteToggle = {},
        onSmsChecked = {},
        onEmailChecked = {},
        onPushChecked = {},
    )
}
