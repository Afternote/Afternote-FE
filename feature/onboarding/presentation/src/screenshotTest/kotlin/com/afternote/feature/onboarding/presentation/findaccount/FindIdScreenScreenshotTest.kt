package com.afternote.feature.onboarding.presentation.findaccount

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [FindIdScreen] 의 시각 회귀 baseline — 아이디 찾기 1단계 (이메일 인증).
 *
 * 시안의 세 상태를 가드:
 * 1. 초기 진입 — 인증번호 필드에 "확인" 없음
 * 2. 인증번호 전송됨 — "확인" 노출 + 파란 안내 2줄
 * 3. 인증번호 불일치 — 빨간 에러 2줄 (안내와 배타)
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findIdScreenInitialScreenshot() {
    AfternoteTheme {
        FindIdScreen(
            initialEmail = "",
            initialCertificateCode = "",
            isSendingCode = false,
            isVerificationSent = false,
            isSendCodeEnabled = false,
            isVerifyEnabled = false,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            hasVerificationError = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onVerifyCode = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findIdScreenCodeSentScreenshot() {
    AfternoteTheme {
        FindIdScreen(
            initialEmail = "parkchae01@gmail.com",
            initialCertificateCode = "",
            isSendingCode = false,
            isVerificationSent = true,
            isSendCodeEnabled = true,
            isVerifyEnabled = false,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            hasVerificationError = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onVerifyCode = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findIdScreenCodeMismatchScreenshot() {
    AfternoteTheme {
        FindIdScreen(
            initialEmail = "parkchae01@gmail.com",
            initialCertificateCode = "123456",
            isSendingCode = false,
            isVerificationSent = true,
            isSendCodeEnabled = true,
            isVerifyEnabled = true,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            hasVerificationError = true,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onVerifyCode = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
