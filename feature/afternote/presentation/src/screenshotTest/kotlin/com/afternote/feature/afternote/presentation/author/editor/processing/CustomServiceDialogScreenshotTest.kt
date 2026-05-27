package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun customServiceDialogScreenshot() {
    AfternoteTheme {
        CustomServiceDialog(
            serviceNameState = rememberTextFieldState(),
            onDismiss = {},
            onAddClick = {},
        )
    }
}
