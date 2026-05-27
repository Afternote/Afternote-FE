package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [SignUpScreen] 의 시각 회귀 baseline — 회원가입 Step 1 (이메일 + 인증번호).
 *
 * 두 케이스로 화면 흐름의 두 핵심 상태를 가드:
 * 1. 초기 진입 — 빈 입력 + "인증번호 받기" 비활성
 * 2. 인증번호 발송 + timer 진행 중 — 재전송 쿨다운 + 만료 카운트다운 시각 가드
 *
 * timer state 는 static 값으로 캡처. animation 자체 회귀는 instrumented 영역.
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun signUpScreenInitialScreenshot() {
    AfternoteTheme {
        SignUpScreen(
            initialEmail = "",
            initialVerificationCode = "",
            isVerificationSent = false,
            isSendingCode = false,
            isEmailFormatValid = false,
            resendCooldownSeconds = 0,
            verificationRemainingSeconds = 0,
            isNextEnabled = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onVerificationCodeChange = {},
            onRequestVerification = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun signUpScreenVerificationInProgressScreenshot() {
    AfternoteTheme {
        SignUpScreen(
            initialEmail = "user@example.com",
            initialVerificationCode = "",
            isVerificationSent = true,
            isSendingCode = false,
            isEmailFormatValid = true,
            resendCooldownSeconds = 20,
            verificationRemainingSeconds = 120,
            isNextEnabled = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onVerificationCodeChange = {},
            onRequestVerification = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
