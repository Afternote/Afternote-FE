package com.afternote.feature.setting.presentation.passkey

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.R
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun passkeyListLoadingScreenshot() {
    PasskeyListScreenshotContent(isLoading = true)
}

@PreviewTest
@Preview(showBackground = true)
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun passkeyListErrorScreenshot() {
    PasskeyListScreenshotContent(errorMessage = stringResource(R.string.setting_passkey_list_error))
}

@PreviewTest
@Preview(showBackground = true)
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun passkeyListEmptyScreenshot() {
    PasskeyListScreenshotContent()
}

@PreviewTest
@Preview(showBackground = true)
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun passkeyListRegisteredScreenshot() {
    PasskeyListScreenshotContent(
        passkeys =
            listOf(
                Passkey(7L, "휴대전화 패스키", "2026-09-06T10:00:00"),
                Passkey(8L, "태블릿 패스키", "2026-09-08T15:30:00"),
            ),
    )
}

@Composable
private fun PasskeyListScreenshotContent(
    passkeys: List<Passkey> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    AfternoteTheme {
        PassKeyListScreen(
            passkeys = passkeys,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onBackClick = {},
            onRegisterClick = {},
            onRetryClick = {},
        )
    }
}
