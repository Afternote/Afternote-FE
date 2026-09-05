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
    PushNotificationScreenshotContent(PushNotificationUiState(isLoading = false, isDeviceAlarmOn = true))
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun pushNotificationAlarmOffScreenshot() {
    PushNotificationScreenshotContent(PushNotificationUiState(isLoading = false, isDeviceAlarmOn = false))
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun pushNotificationAlarmOffCompactScreenshot() {
    PushNotificationScreenshotContent(PushNotificationUiState(isLoading = false, isDeviceAlarmOn = false))
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun pushNotificationLoadErrorScreenshot() {
    PushNotificationScreenshotContent(
        PushNotificationUiState(isLoading = false, errorMessage = "load error"),
    )
}

@Composable
private fun PushNotificationScreenshotContent(uiState: PushNotificationUiState) {
    AfternoteTheme {
        PushNotificationContent(
            uiState = uiState,
            onBack = {},
            onDeviceAlarmClick = {},
            onNewsletterToggle = {},
            onMindRecordToggle = {},
            onAfternoteToggle = {},
            onSmsCheck = {},
            onEmailCheck = {},
            onPushCheck = {},
            onRetry = {},
        )
    }
}
