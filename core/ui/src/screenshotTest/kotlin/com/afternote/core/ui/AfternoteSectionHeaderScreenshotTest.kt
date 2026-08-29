package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteSectionHeader] 의 시각 회귀 baseline.
 *
 * 이 함수가 해당 컴포저블의 유일한 `@Preview` 다 — main 의 중복 프리뷰는 삭제됐다 (#1434).
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
