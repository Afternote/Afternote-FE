package com.afternote.feature.onboarding.presentation.login

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [LoginScreen] 의 시각 회귀 baseline — 초기 진입 (빈 입력, 비로딩).
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun loginScreenInitialScreenshot() {
    AfternoteTheme {
        LoginScreen(
            initialEmail = "",
            initialPassword = "",
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onSignUpClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() },
            isLoading = false,
        )
    }
}
