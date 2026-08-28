package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.button.AfternoteRadioGroup
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteRadioGroup]의 선택/비선택 두 케이스 baseline.
 *
 * baseline은 GitHub Actions의 screenshot-baseline workflow에서 갱신합니다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun customRadioButtonSelectedScreenshot() {
    AfternoteTheme {
        AfternoteRadioGroup(
            options = listOf(Unit),
            selectedValue = Unit,
            onSelect = {},
        ) { _, _ -> }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun customRadioButtonUnselectedScreenshot() {
    AfternoteTheme {
        AfternoteRadioGroup(
            options = listOf(Unit),
            selectedValue = null,
            onSelect = {},
        ) { _, _ -> }
    }
}
