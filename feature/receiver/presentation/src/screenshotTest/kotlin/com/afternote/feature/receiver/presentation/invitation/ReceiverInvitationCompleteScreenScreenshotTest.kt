package com.afternote.feature.receiver.presentation.invitation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.COMPACT_DEVICE_SPEC
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverInvitationCompleteScreenshot() {
    AfternoteTheme {
        ReceiverInvitationCompleteScreen(inviterName = "김혜성", onConfirm = {})
    }
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverInvitationCompleteCompactScreenshot() {
    AfternoteTheme {
        ReceiverInvitationCompleteScreen(inviterName = "김혜성", onConfirm = {})
    }
}
