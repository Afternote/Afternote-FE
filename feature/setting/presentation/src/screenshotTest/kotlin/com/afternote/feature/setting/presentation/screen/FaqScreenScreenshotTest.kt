package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun faqScreenScreenshot() {
    FaqScreenScreenshotContent()
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun faqScreenCompactScreenshot() {
    FaqScreenScreenshotContent()
}

@Composable
private fun FaqScreenScreenshotContent() {
    AfternoteTheme {
        FaqScreen(onBackClick = {})
    }
}
