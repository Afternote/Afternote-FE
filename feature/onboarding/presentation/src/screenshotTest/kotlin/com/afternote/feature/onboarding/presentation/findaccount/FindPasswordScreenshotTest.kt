package com.afternote.feature.onboarding.presentation.findaccount

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.onboarding.presentation.LARGE_FONT_SCALE
import com.android.tools.screenshot.PreviewTest

/**
 * 비밀번호 찾기 세 화면의 시각 회귀 baseline (#457).
 *
 * 시안 5장을 그대로 가드한다:
 * 1. 이메일 인증 초기 (`2431:14299`)
 * 2. 인증번호 전송됨 — 파란 안내 2줄 (`2383:16680`)
 * 3. 비밀번호 변경 (`2383:16789`)
 * 4. 변경 완료 (`2383:16854`)
 *
 * 소셜 차단 팝업(`2383:16667`)은 [androidx.compose.ui.window.Dialog] 라 별도 윈도우에 그려져
 * 프리뷰 캡처에 담기지 않는다 — 표시 조건은 `FindPasswordViewModelTest` 가 상태로 가드한다.
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findPasswordScreenInitialScreenshot() {
    AfternoteTheme {
        FindPasswordScreen(
            initialEmail = "",
            initialCertificateCode = "",
            isSendingCode = false,
            isVerificationSent = false,
            isSendCodeEnabled = false,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            showSocialAccountBlockedPopup = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onSocialAccountBlockedConfirm = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findPasswordScreenCodeSentScreenshot() {
    AfternoteTheme {
        FindPasswordScreen(
            initialEmail = "parkchae01@gmail.com",
            initialCertificateCode = "",
            isSendingCode = false,
            isVerificationSent = true,
            isSendCodeEnabled = true,
            isNextEnabled = false,
            resendCooldownSeconds = 0,
            showSocialAccountBlockedPopup = false,
            snackbarHostState = remember { SnackbarHostState() },
            onEmailChange = {},
            onCertificateCodeChange = {},
            onRequestCode = {},
            onSocialAccountBlockedConfirm = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findPasswordResetScreenInitialScreenshot() {
    AfternoteTheme {
        FindPasswordResetScreen(
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

/**
 * 규칙 충족 변형 — 첫 줄 안내가 회색(gray5)에서 강조색(b1)으로 갈리는 것이 이 화면의 유일한
 * 상태 변화다. 색만 바뀌는 축이라 초기 baseline 으로는 잡히지 않는다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findPasswordResetScreenRuleSatisfiedScreenshot() {
    AfternoteTheme {
        FindPasswordResetScreen(
            initialPassword = "NewPass1!",
            initialPasswordConfirm = "NewPass1!",
            isPasswordRuleSatisfied = true,
            isNextEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 스크롤이 붙어 있어도 안내 2줄이 접히면 시안과 어긋난다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun findPasswordResetScreenCompactScreenshot() {
    AfternoteTheme {
        FindPasswordResetScreen(
            initialPassword = "NewPass1!",
            initialPasswordConfirm = "NewPass1!",
            isPasswordRuleSatisfied = true,
            isNextEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

/**
 * 글자 확대(×1.5) 변형 — 안내 2줄이 가장 먼저 넘치는 자리다. 기준값은 [LARGE_FONT_SCALE].
 */
@PreviewTest
@Preview(showBackground = true, fontScale = LARGE_FONT_SCALE)
@Composable
internal fun findPasswordResetScreenLargeFontScreenshot() {
    AfternoteTheme {
        FindPasswordResetScreen(
            initialPassword = "NewPass1!",
            initialPasswordConfirm = "NewPass1!",
            isPasswordRuleSatisfied = true,
            isNextEnabled = true,
            snackbarHostState = remember { SnackbarHostState() },
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun findPasswordCompleteScreenScreenshot() {
    AfternoteTheme {
        FindPasswordCompleteScreen(onLoginClick = {})
    }
}
