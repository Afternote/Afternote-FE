package com.afternote.feature.afternote.presentation.editor.account

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.fixture.sampleAccountSection
import com.afternote.feature.afternote.presentation.editor.fixture.sampleProcessingMethodSection
import com.afternote.feature.afternote.presentation.editor.fixture.sampleRecipientSection
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun accountEditorContentWithRecipientScreenshot() {
    AccountEditorContentScreenshotFixture(hasRecipient = true)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun accountEditorContentWithoutRecipientScreenshot() {
    AccountEditorContentScreenshotFixture(hasRecipient = false)
}

@Composable
private fun AccountEditorContentScreenshotFixture(hasRecipient: Boolean) {
    AfternoteTheme {
        AccountEditorContent(
            editorMessages = emptyList(),
            accountSection = sampleAccountSection(),
            recipientSection = sampleRecipientSection(hasRecipient = hasRecipient),
            processingMethodSection = sampleProcessingMethodSection(),
            onMessageRegisterClick = {},
            onMessageDeleteClick = {},
            onMessageAddClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
