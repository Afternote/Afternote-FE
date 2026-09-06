package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.R
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

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun pushNotificationLoadErrorScreenshot() {
    PushNotificationScreenshotContent(
        PushNotificationUiState(isLoading = false, errorMessage = UiText.Resource(R.string.setting_push_load_error)),
    )
}

@Composable
private fun PushNotificationScreenshotContent(
    uiState: PushNotificationUiState = PushNotificationUiState(isLoading = false, isAfternoteOn = true),
) {
    AfternoteTheme {
        PushNotificationContent(
            uiState = uiState,
            onBack = {},
            onNewsletterToggle = {},
            onMindRecordToggle = {},
            onAfternoteToggle = {},
            onRetry = {},
        )
    }
}
