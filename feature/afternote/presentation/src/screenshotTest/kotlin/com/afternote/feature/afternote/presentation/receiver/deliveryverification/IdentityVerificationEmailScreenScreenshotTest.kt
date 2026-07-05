package com.afternote.feature.afternote.presentation.receiver.deliveryverification

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
internal fun identityVerificationEmailScreenEmptyScreenshot() {
    AfternoteTheme {
        IdentityVerificationEmailScreenContent(
            uiState = IdentityVerificationUiState(),
            emailState = rememberTextFieldState(),
            codeState = rememberTextFieldState(),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onRequestCode = {},
            onVerifyAndProceed = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun identityVerificationEmailScreenSentScreenshot() {
    AfternoteTheme {
        IdentityVerificationEmailScreenContent(
            uiState =
                IdentityVerificationUiState(
                    email = "user@example.com",
                    isEmailFormatValid = true,
                    isVerificationSent = true,
                ),
            emailState = rememberTextFieldState("user@example.com"),
            codeState = rememberTextFieldState(),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onRequestCode = {},
            onVerifyAndProceed = {},
        )
    }
}
