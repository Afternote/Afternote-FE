package com.afternote.feature.receiver.presentation.senderdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun senderDetailScreenNotRequestedScreenshot() {
    AfternoteTheme {
        SenderDetailScreenContent(
            uiState =
                SenderDetailUiState.Success(
                    displayName = "김혜성",
                    verification = SenderVerificationState.NotRequested,
                    requestedAt = null,
                    approvedAt = null,
                ),
            onBackClick = {},
            onRequestVerification = {},
            onOpenReceiverHome = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun senderDetailScreenPendingScreenshot() {
    AfternoteTheme {
        SenderDetailScreenContent(
            uiState =
                SenderDetailUiState.Success(
                    displayName = "김혜성",
                    verification = SenderVerificationState.Pending,
                    requestedAt = "2026.05.03.",
                    approvedAt = null,
                ),
            onBackClick = {},
            onRequestVerification = {},
            onOpenReceiverHome = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun senderDetailScreenApprovedScreenshot() {
    AfternoteTheme {
        SenderDetailScreenContent(
            uiState =
                SenderDetailUiState.Success(
                    displayName = "김혜성",
                    verification = SenderVerificationState.Approved,
                    requestedAt = "2026.05.03.",
                    approvedAt = "2026.05.03.",
                ),
            onBackClick = {},
            onRequestVerification = {},
            onOpenReceiverHome = {},
        )
    }
}
