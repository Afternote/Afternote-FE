package com.afternote.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [CaptionLabeledTextField] 의 시각 회귀 baseline — 라벨 + 빈 TextFieldState (placeholder 표시).
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun captionLabeledTextFieldScreenshot() {
    AfternoteTheme {
        CaptionLabeledTextField(
            label = "이메일",
            state = rememberTextFieldState(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
