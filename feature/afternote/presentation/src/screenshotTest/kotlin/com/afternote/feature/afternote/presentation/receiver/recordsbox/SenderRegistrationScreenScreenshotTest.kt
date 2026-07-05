package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun senderRegistrationScreenEmptyScreenshot() {
    AfternoteTheme {
        SenderRegistrationScreenContent(
            nameState = rememberTextFieldState(),
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun senderRegistrationScreenFilledScreenshot() {
    AfternoteTheme {
        SenderRegistrationScreenContent(
            nameState = rememberTextFieldState("Text Field"),
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}
