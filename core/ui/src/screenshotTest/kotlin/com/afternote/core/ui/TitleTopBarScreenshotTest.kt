package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.TitleTopBar
import com.android.tools.screenshot.PreviewTest

/**
 * [TitleTopBar] 의 시각 회귀 baseline — 좌측 타이틀 + actions 없는 기본 케이스.
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun titleTopBarScreenshot() {
    AfternoteTheme {
        TitleTopBar(title = "타이틀")
    }
}
