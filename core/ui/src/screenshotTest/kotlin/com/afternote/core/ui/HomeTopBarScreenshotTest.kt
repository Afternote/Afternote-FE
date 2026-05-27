package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.android.tools.screenshot.PreviewTest

/**
 * [HomeTopBar] 의 시각 회귀 baseline — 좌측 로고 + 우측 user/setting icon.
 * 입력 무관 stable 출력.
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun homeTopBarScreenshot() {
    AfternoteTheme {
        HomeTopBar()
    }
}
