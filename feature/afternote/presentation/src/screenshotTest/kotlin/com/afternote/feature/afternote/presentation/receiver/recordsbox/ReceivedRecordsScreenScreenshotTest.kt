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
                    screenshotRecordItem(recordBoxId = 1L),
                    screenshotRecordItem(recordBoxId = 2L),
                    screenshotRecordItem(recordBoxId = 3L),
                    screenshotRecordItem(recordBoxId = 4L),
                ),
            onBackClick = {},
            onSenderClick = {},
        )
    }
}

private fun screenshotRecordItem(recordBoxId: Long): ReceivedRecordItem =
    ReceivedRecordItem(
        recordBoxId = recordBoxId,
        accessCode = "screenshot-key-$recordBoxId",
        senderName = "김혜성",
        receiverName = "김지은",
        relation = "DAUGHTER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = ReceivedRecordViewStatus.Requestable,
        verification = ReceivedRecordVerification.NotRequested,
    )
