package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receivedRecordsScreenEmptyScreenshot() {
    AfternoteTheme {
        ReceivedRecordsScreenContent(
            senders = emptyList(),
            onBackClick = {},
            onAddSenderClick = {},
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
                    SenderEntry(id = "1", name = "김혜성"),
                    SenderEntry(id = "2", name = "김혜성"),
                    SenderEntry(id = "3", name = "김혜성"),
                    SenderEntry(id = "4", name = "김혜성"),
                ),
            onBackClick = {},
            onAddSenderClick = {},
            onSenderClick = {},
        )
    }
}
