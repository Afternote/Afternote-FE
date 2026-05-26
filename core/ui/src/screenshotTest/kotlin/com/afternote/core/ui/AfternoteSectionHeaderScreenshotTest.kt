package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteSectionHeader] 의 시각 회귀 baseline.
 *
 * main 의 `@Preview` 함수는 AS 미리보기 용도로 유지. 본 함수는 *baseline PNG 생성용*.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteSectionHeaderScreenshot() {
    AfternoteTheme {
        AfternoteSectionHeader(title = "NEXT STEP")
    }
}
