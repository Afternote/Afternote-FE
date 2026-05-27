package com.afternote.feature.onboarding.presentation.terms

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [TermsDetailScreen] 의 시각 회귀 baseline — 약관 상세 화면 진입.
 *
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun termsDetailScreenScreenshot() {
    AfternoteTheme {
        TermsDetailScreen(
            title = "이용약관",
            onBackClick = {},
            onNextClick = {},
        )
    }
}
