package com.afternote.feature.afternote.presentation.receiver.deliveryverification.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DocumentSlotState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun documentSlotCardEmptyScreenshot() {
    AfternoteTheme {
        DocumentSlotCard(
            title = "사망진단서 업로드",
            slot = DocumentSlotState(),
            onPickClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun documentSlotCardFilledScreenshot() {
    AfternoteTheme {
        DocumentSlotCard(
            title = "사망진단서 업로드",
            slot = DocumentSlotState(displayName = "사망진단서.jpeg", fileUrl = "https://example.com/x"),
            onPickClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
