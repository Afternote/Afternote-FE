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
 * 세 케이스로 화면 흐름의 핵심 상태를 가드:
 * 1. 초기 진입 — 빈 입력 + "인증번호 받기" 비활성
 * 2. 인증번호 발송 후 — 재전송 쿨다운 + 전송 안내 문구
 * 3. 인증번호 불일치 — 필드 아래 인라인 에러 문구 (시안 2431-14204)
 *
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
            hasVerificationError = false,
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
            hasVerificationError = false,
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
internal fun signUpScreenVerificationMismatchScreenshot() {
    AfternoteTheme {
        SignUpScreen(
            initialEmail = "user@example.com",
            initialVerificationCode = "000000",
            isVerificationSent = true,
            isSendingCode = false,
            isEmailFormatValid = true,
            resendCooldownSeconds = 0,
            hasVerificationError = true,
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
