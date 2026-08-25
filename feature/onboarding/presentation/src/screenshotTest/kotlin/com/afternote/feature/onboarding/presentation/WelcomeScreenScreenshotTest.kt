package com.afternote.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [WelcomeScreen] 의 시각 회귀 baseline — 진입 화면 (시작 / 기록 확인 / 로그인 3개 액션).
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun welcomeScreenScreenshot() {
    AfternoteTheme {
        WelcomeScreen(
            onStartClick = {},
            onCheckRecordsClick = {},
            onLoginClick = {},
        )
    }
}

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 스크롤이 없는 화면이라 세로가 모자라면 그대로 잘린다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun welcomeScreenCompactScreenshot() {
    AfternoteTheme {
        WelcomeScreen(
            onStartClick = {},
            onCheckRecordsClick = {},
            onLoginClick = {},
        )
    }
}
