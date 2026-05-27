package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [KoreanConsonantIndex] 의 시각 회귀 baseline — 한글 14자 세로 인덱스.
 *
 * `selectedConsonant = null` (전체 unselected) + `selectedConsonant = 'ㄱ'` (특정 하나 selected)
 * 두 케이스로 선택 표시 시각 회귀 가드.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun koreanConsonantIndexUnselectedScreenshot() {
    AfternoteTheme {
        KoreanConsonantIndex(
            selectedConsonant = null,
            onConsonantSelect = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun koreanConsonantIndexSelectedScreenshot() {
    AfternoteTheme {
        KoreanConsonantIndex(
            selectedConsonant = 'ㄱ',
            onConsonantSelect = {},
        )
    }
}
