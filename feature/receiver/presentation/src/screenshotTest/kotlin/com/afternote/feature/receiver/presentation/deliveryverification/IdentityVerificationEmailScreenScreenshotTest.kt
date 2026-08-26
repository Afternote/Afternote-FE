package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.receiver.presentation.LARGE_FONT_SCALE
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

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 스크롤이 없는 화면이라 세로가 모자라면 그대로 잘린다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun identityVerificationEmailScreenSentCompactScreenshot() {
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

/**
 * 글자 확대(×1.5) 변형 — 본인 확인 이메일 — 발송됨.
 *
 * 화면 크기와 다른 축이라 좁은 화면 baseline 으로는 잡히지 않는다. 기준값은 [LARGE_FONT_SCALE].
 */
@PreviewTest
@Preview(showBackground = true, fontScale = LARGE_FONT_SCALE)
@Composable
internal fun identityVerificationEmailScreenSentLargeFontScreenshot() {
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
