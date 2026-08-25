package com.afternote.feature.receiver.presentation.senderdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.COMPACT_DEVICE_SPEC
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

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 스크롤이 없는 화면이라 세로가 모자라면 그대로 잘린다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun senderDetailScreenApprovedCompactScreenshot() {
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
