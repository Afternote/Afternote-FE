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
