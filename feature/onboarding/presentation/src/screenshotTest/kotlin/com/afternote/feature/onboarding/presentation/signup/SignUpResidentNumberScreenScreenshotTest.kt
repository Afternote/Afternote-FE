package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [SignUpResidentNumberScreen] 의 시각 회귀 baseline — 초기 진입 상태 (빈 입력 + 다음 비활성).
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun signUpResidentNumberScreenInitialScreenshot() {
    AfternoteTheme {
        SignUpResidentNumberScreen(
            initialFrontNumber = "",
            initialBackNumber = "",
            isNextEnabled = false,
            snackbarHostState = remember { SnackbarHostState() },
            onFrontNumberChange = {},
            onBackNumberChange = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
