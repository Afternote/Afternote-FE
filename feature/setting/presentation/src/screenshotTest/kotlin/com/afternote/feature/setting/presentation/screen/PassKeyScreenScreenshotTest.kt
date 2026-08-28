package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun passKeyScreenScreenshot() {
    PassKeyScreenScreenshotContent()
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun passKeyScreenCompactScreenshot() {
    PassKeyScreenScreenshotContent()
}

@Composable
private fun PassKeyScreenScreenshotContent() {
    AfternoteTheme {
        PassKeyScreen(
            onBackClick = {},
            onRegisterClick = {},
        )
    }
}
