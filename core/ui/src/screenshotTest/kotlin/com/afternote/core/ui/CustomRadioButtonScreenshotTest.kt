package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.button.CustomRadioButton
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [CustomRadioButton] 의 선택/비선택 두 케이스 baseline.
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun customRadioButtonSelectedScreenshot() {
    AfternoteTheme {
        CustomRadioButton(selected = true)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun customRadioButtonUnselectedScreenshot() {
    AfternoteTheme {
        CustomRadioButton(selected = false)
    }
}
