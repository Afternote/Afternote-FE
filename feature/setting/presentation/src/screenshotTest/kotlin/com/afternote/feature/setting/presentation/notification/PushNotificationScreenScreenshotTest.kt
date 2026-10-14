package com.afternote.feature.setting.presentation.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.R
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
private fun PushNotificationScreenshotContent(uiState: PushNotificationUiState = PushNotificationUiState(isAfternoteOn = true)) {
    AfternoteTheme {
        PushNotificationContent(
            onRetry = {},
            uiState = uiState,
            onBack = {},
            onNewsletterToggle = {},
            onMindRecordToggle = {},
            onAfternoteToggle = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun pushNotificationLoadingScreenshot() {
    PushNotificationScreenshotContent(PushNotificationUiState(isLoading = true))
}

@PreviewTest
@Preview(showBackground = true)
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun pushNotificationLoadErrorScreenshot() {
    PushNotificationScreenshotContent(PushNotificationUiState(errorMessage = UiText.Resource(R.string.setting_push_load_error)))
}
