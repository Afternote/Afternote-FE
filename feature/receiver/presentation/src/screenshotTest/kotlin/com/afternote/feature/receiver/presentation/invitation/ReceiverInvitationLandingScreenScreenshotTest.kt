package com.afternote.feature.receiver.presentation.invitation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.UiText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.receiver.presentation.LARGE_FONT_SCALE
import com.afternote.feature.receiver.presentation.R
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverInvitationLandingReadyScreenshot() {
    LandingScreenshot(ReceiverInvitationUiState(phase = ReceiverInvitationPhase.Ready("김혜성")))
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverInvitationLandingReadyCompactScreenshot() {
    LandingScreenshot(ReceiverInvitationUiState(phase = ReceiverInvitationPhase.Ready("김혜성")))
}

@PreviewTest
@Preview(showBackground = true, fontScale = LARGE_FONT_SCALE)
@Composable
internal fun receiverInvitationLandingReadyLargeFontScreenshot() {
    LandingScreenshot(ReceiverInvitationUiState(phase = ReceiverInvitationPhase.Ready("김혜성")))
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverInvitationLandingNoticeScreenshot() {
    LandingScreenshot(
        ReceiverInvitationUiState(
            phase = ReceiverInvitationPhase.Notice(UiText.Resource(R.string.receiver_invitation_notice_expired)),
        ),
    )
}

@Composable
private fun LandingScreenshot(state: ReceiverInvitationUiState) {
    AfternoteTheme {
        ReceiverInvitationLandingContent(state = state, onIntent = {})
    }
}
