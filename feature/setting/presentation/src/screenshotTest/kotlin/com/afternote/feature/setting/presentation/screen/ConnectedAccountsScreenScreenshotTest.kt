package com.afternote.feature.setting.presentation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsUiState
import com.afternote.feature.setting.presentation.viewmodel.SocialAccountState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun connectedAccountsScreenScreenshot() {
    ConnectedAccountsScreenScreenshotContent()
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun connectedAccountsScreenCompactScreenshot() {
    ConnectedAccountsScreenScreenshotContent()
}

@Composable
private fun ConnectedAccountsScreenScreenshotContent() {
    AfternoteTheme {
        ConnectedAccountsContent(
            uiState = connectedAccountsPreviewState,
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onToggle = { _, _ -> },
            onRetry = {},
        )
    }
}

private val connectedAccountsPreviewState =
    ConnectedAccountsUiState(
        accounts =
            listOf(
                SocialAccountState(
                    provider = "naver",
                    iconRes = R.drawable.ic_naver_logo,
                    labelRes = R.string.login_with_naver,
                    isConnected = false,
                    isLinkable = false,
                ),
                SocialAccountState(
                    provider = "google",
                    iconRes = R.drawable.ic_google_logo,
                    labelRes = R.string.login_with_google,
                    isConnected = true,
                    email = "example@gmail.com",
                ),
                SocialAccountState(
                    provider = "kakao",
                    iconRes = R.drawable.ic_kakao_logo,
                    labelRes = R.string.login_with_kakao,
                    isConnected = false,
                ),
                SocialAccountState(
                    provider = "apple",
                    iconRes = R.drawable.ic_apple_logo,
                    labelRes = R.string.login_with_apple,
                    isConnected = false,
                    isLinkable = false,
                ),
            ),
    )
