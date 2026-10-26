package com.afternote.feature.setting.presentation.receiver.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.R
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverInviteSheetContentScreenshot() {
    AfternoteTheme {
        ReceiverInviteSheetContent(
            receiverName = "김민서",
            isSending = false,
            errorMessage = null,
            onSend = {},
            onDismiss = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverInviteSheetContentErrorScreenshot() {
    AfternoteTheme {
        ReceiverInviteSheetContent(
            receiverName = "김민서",
            isSending = false,
            errorMessage = UiText.Resource(R.string.setting_receiver_invite_share_failed),
            onSend = {},
            onDismiss = {},
        )
    }
}
