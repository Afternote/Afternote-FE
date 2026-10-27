package com.afternote.feature.setting.presentation.notification

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.NotificationPreferences
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.notification.preference.NotificationPreferenceUiState
import com.afternote.feature.setting.presentation.notification.preference.NotificationTimeSection
import com.android.tools.screenshot.PreviewTest
import java.time.DayOfWeek
import java.time.LocalTime

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

/** Figma 「설정 - 푸시알림 설정」 — 토글 아래 알림 시간 설정 구역 (#2321). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun pushNotificationWithTimeScreenshot() {
    AfternoteTheme {
        PushNotificationContent(
            onRetry = {},
            uiState = PushNotificationUiState(isAfternoteOn = true),
            onBack = {},
            onNewsletterToggle = {},
            onMindRecordToggle = {},
            onAfternoteToggle = {},
            timeSection = {
                NotificationTimeSection(
                    uiState =
                        NotificationPreferenceUiState(
                            isLoading = false,
                            preferences = NotificationPreferences(true, LocalTime.of(9, 0), DayOfWeek.entries.toSet(), 3),
                        ),
                    onIntent = {},
                )
            },
        )
    }
}
