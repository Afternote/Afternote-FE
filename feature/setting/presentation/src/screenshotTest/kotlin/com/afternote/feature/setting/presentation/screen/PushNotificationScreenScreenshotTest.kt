package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun pushNotificationAlarmOnScreenshot() {
    PushNotificationScreenshotContent(isDeviceAlarmOn = true)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun pushNotificationAlarmOffScreenshot() {
    PushNotificationScreenshotContent(isDeviceAlarmOn = false)
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun pushNotificationAlarmOffCompactScreenshot() {
    PushNotificationScreenshotContent(isDeviceAlarmOn = false)
}

@Composable
private fun PushNotificationScreenshotContent(isDeviceAlarmOn: Boolean) {
    AfternoteTheme {
        PushNotificationContent(
            uiState = PushNotificationUiState(isDeviceAlarmOn = isDeviceAlarmOn),
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
}
