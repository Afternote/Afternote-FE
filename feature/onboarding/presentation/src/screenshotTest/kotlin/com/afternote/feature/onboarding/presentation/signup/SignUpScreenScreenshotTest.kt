package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [SignUpContent] 의 시각 회귀 baseline — 회원가입 Step 1 (이메일 + 인증번호).
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
        SignUpContent(
            state = SignUpUiState(),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun signUpScreenVerificationInProgressScreenshot() {
    AfternoteTheme {
        SignUpContent(
            state =
                SignUpUiState(
                    email = "user@example.com",
                    isVerificationSent = true,
                    resendCooldownSeconds = 20,
                ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun signUpScreenVerificationMismatchScreenshot() {
    AfternoteTheme {
        SignUpContent(
            state =
                SignUpUiState(
                    email = "user@example.com",
                    verificationCode = "000000",
                    isVerificationSent = true,
                    // baseline 은 «다음» 이 비활성인 채로 잡혀 있다. isNextEnabled 가 파생값이 된
                    // 지금 그 픽셀을 내는 상태는 검증이 아직 진행 중인 프레임이다 — 서버가 무효를
                    // 알려 hasVerificationError 가 서고(VerificationRejected) 요청이 끝나
                    // isVerifyingEmail 이 내려가기(EmailVerifyFinished) 전 사이다.
                    isVerifyingEmail = true,
                    hasVerificationError = true,
                ),
            onIntent = {},
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
        )
    }
}
