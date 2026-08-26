package com.afternote.feature.onboarding.presentation.terms

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.COMPACT_DEVICE_SPEC
import com.android.tools.screenshot.PreviewTest

/**
 * [OnboardingTermsScreen] 의 시각 회귀 baseline — 초기 진입 (모든 약관 미동의, 다음 비활성).
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun onboardingTermsScreenInitialScreenshot() {
    AfternoteTheme {
        OnboardingTermsScreen(
            termsState = TermsState(),
            isNextEnabled = false,
            snackbarHostState = remember { SnackbarHostState() },
            onTermsToggle = {},
            onPrivacyToggle = {},
            onMarketingToggle = {},
            onToggleAll = {},
            onViewTermsClick = {},
            onNextClick = {},
            onBackClick = {},
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
internal fun onboardingTermsScreenInitialCompactScreenshot() {
    AfternoteTheme {
        OnboardingTermsScreen(
            termsState = TermsState(),
            isNextEnabled = false,
            snackbarHostState = remember { SnackbarHostState() },
            onTermsToggle = {},
            onPrivacyToggle = {},
            onMarketingToggle = {},
            onToggleAll = {},
            onViewTermsClick = {},
            onNextClick = {},
            onBackClick = {},
        )
    }
}
