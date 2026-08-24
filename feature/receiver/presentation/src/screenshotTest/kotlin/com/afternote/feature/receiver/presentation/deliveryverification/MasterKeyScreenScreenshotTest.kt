package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun masterKeyScreenEmptyScreenshot() {
    AfternoteTheme {
        MasterKeyScreenContent(
            authCodeState = rememberTextFieldState(),
            isSubmitting = false,
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun masterKeyScreenFilledScreenshot() {
    AfternoteTheme {
        MasterKeyScreenContent(
            authCodeState = rememberTextFieldState("ABC-DEF-GHI"),
            isSubmitting = false,
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onSubmitClick = {},
        )
    }
}
