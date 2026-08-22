package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receivedRecordsScreenEmptyScreenshot() {
    AfternoteTheme {
        ReceivedRecordsScreenContent(
            senders = emptyList(),
            onBackClick = {},
            onSenderClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receivedRecordsScreenFilledScreenshot() {
    AfternoteTheme {
        ReceivedRecordsScreenContent(
            senders =
                listOf(
                    screenshotRecordItem(receiverId = 1L),
                    screenshotRecordItem(receiverId = 2L),
                    screenshotRecordItem(receiverId = 3L),
                    screenshotRecordItem(receiverId = 4L),
                ),
            onBackClick = {},
            onSenderClick = {},
        )
    }
}

private fun screenshotRecordItem(receiverId: Long): ReceivedRecordItem =
    ReceivedRecordItem(
        receiverId = receiverId,
        accessCode = "screenshot-key-$receiverId",
        senderName = "김혜성",
        receiverName = "김지은",
        relation = "DAUGHTER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = ReceivedRecordViewStatus.Requestable,
        verification = ReceivedRecordVerification.NotRequested,
    )
