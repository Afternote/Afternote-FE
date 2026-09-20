package com.afternote.feature.setting.presentation.receiver

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverInviteSentScreenScreenshot() {
    ReceiverInviteSentScreenshotContent()
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverInviteSentScreenCompactScreenshot() {
    ReceiverInviteSentScreenshotContent()
}

@Composable
private fun ReceiverInviteSentScreenshotContent() {
    AfternoteTheme {
        ReceiverInviteSentContent(
            receiverName = "김민서",
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onOpenReceiverList = {},
            onResend = {},
        )
    }
}
