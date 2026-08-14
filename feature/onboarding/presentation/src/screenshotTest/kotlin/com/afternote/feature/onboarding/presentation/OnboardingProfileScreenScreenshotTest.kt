package com.afternote.feature.onboarding.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [OnboardingProfileScreen] 의 시각 회귀 baseline — 초기 진입 상태 (이름 미입력 + 이미지 미선택).
 *
 * stateful 흐름 Screen 의 첫 baseline. `photoPickerLauncher` 는 Preview 환경에서 placeholder
 * 동작 (실제 picker 실행 없음) 이라 stable 렌더.
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun onboardingProfileScreenInitialScreenshot() {
    AfternoteTheme {
        OnboardingProfileScreen(
            initialName = "",
            displayImageUri = null,
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange = {},
            onProfileImagePick = {},
            onBackClick = {},
            onCompleteClick = {},
        )
    }
}

/**
 * 제출 진행 중 — 완료 버튼이 스피너로 바뀌고 비활성 스타일로 잠긴다.
 *
 * 이름이 채워져 있어도 잠기는지가 이 baseline 의 요점이다. 잠금이 풀리면 연타·IME 로
 * 회원가입이 중복 호출된다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun onboardingProfileScreenSubmittingScreenshot() {
    AfternoteTheme {
        OnboardingProfileScreen(
            initialName = "애프터노트",
            displayImageUri = null,
            snackbarHostState = remember { SnackbarHostState() },
            onNameChange = {},
            onProfileImagePick = {},
            onBackClick = {},
            onCompleteClick = {},
            isSubmitting = true,
        )
    }
}
