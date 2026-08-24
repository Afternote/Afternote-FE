package com.afternote.afternote_fe.screen.receiver.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.afternote_fe.screen.receiver.model.MindRecordSummary
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun mindRecordSectionScreenshot() {
    AfternoteTheme {
        MindRecordSection(
            summary =
                MindRecordSummary(
                    dailyQuestionCount = 10,
                    diaryCount = 8,
                ),
            onGoClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
