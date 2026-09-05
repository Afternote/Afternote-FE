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
internal fun pushNotificationScreenshot() {
    PushNotificationScreenshotContent()
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun pushNotificationCompactScreenshot() {
    PushNotificationScreenshotContent()
}

@Composable
private fun PushNotificationScreenshotContent() {
    AfternoteTheme {
        PushNotificationContent(
            uiState = PushNotificationUiState(isAfternoteOn = true),
            onBack = {},
            onNewsletterToggle = {},
            onMindRecordToggle = {},
            onAfternoteToggle = {},
        )
    }
}
