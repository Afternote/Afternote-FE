package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [SignUpPasswordScreen] 의 시각 회귀 baseline — 초기 진입 상태 (빈 입력 + 비밀번호 룰 미충족 + 다음 비활성).
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun signUpPasswordScreenInitialScreenshot() {
    AfternoteTheme {
        SignUpPasswordScreen(
            initialPassword = "",
            initialPasswordConfirm = "",
            isPasswordRuleSatisfied = false,
            isNextEnabled = false,
            snackbarHostState = remember { SnackbarHostState() },
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
