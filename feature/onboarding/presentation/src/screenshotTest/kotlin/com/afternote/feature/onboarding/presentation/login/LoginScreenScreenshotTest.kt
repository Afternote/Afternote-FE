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
            onFindAccountClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onRetryLogin = {},
            onNetworkErrorDismiss = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() },
            isLoading = false,
        )
    }
}

/**
 * 자격 거절 상태 — 비밀번호 필드 error 보더 + 6dp 아래 인라인 안내 (시안 `3628:23437`).
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun loginScreenCredentialErrorScreenshot() {
    AfternoteTheme {
        LoginScreen(
            initialEmail = "user@example.com",
            initialPassword = "wrong-password",
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onSignUpClick = {},
            onFindAccountClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onRetryLogin = {},
            onNetworkErrorDismiss = {},
            onBackClick = {},
            snackbarHostState = remember { SnackbarHostState() },
            hasCredentialError = true,
        )
    }
}
