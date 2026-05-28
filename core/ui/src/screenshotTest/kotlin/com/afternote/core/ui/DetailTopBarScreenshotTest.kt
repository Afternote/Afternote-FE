package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.android.tools.screenshot.PreviewTest

/**
 * [DetailTopBar] 의 시각 회귀 baseline — 가운데 타이틀 + 좌측 back icon (옵션) + actions slot.
 *
 * `onBackClick = null` (back icon 없음) / non-null (back icon 표시) 두 케이스.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun detailTopBarWithBackScreenshot() {
    AfternoteTheme {
        DetailTopBar(
            title = "상세",
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun detailTopBarWithoutBackScreenshot() {
    AfternoteTheme {
        DetailTopBar(title = "상세")
    }
}
