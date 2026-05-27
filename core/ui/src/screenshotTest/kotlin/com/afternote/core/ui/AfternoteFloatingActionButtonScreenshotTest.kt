package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.button.FAB.AfternoteFloatingActionButton
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteFloatingActionButton] 의 시각 회귀 baseline — 검정 원형 FAB + plus 아이콘 (24dp).
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteFloatingActionButtonScreenshot() {
    AfternoteTheme {
        AfternoteFloatingActionButton(onClick = {})
    }
}
