package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [ProfileImage] 의 시각 회귀 baseline — placeholder fallback 분기.
 *
 * `displayImageUri = null` 케이스만 검증. `AsyncImage` 의 URL 로드 분기는
 * instrumented test 영역이라 본 baseline 에서 제외.
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun profileImagePlaceholderScreenshot() {
    AfternoteTheme {
        ProfileImage()
    }
}
