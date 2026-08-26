package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.COMPACT_DEVICE_SPEC
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

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 스크롤이 없는 화면이라 세로가 모자라면 그대로 잘린다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun masterKeyScreenFilledCompactScreenshot() {
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
