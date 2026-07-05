package com.afternote.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteFieldContainer] 의 시각 회귀 baseline — enabled / disabled 두 상태 (이슈 #342).
 *
 * disabled 일 때 배경(흰색→gray2)·보더(gray2→gray3) 가 비활성 색으로 전환되는지 고정한다.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteFieldContainerEnabledScreenshot() {
    AfternoteTheme {
        AfternoteFieldContainer(
            onClick = {},
            enabled = true,
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            Text(
                text = "활성 필드",
                style = AfternoteDesign.typography.bodyLargeR,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteFieldContainerDisabledScreenshot() {
    AfternoteTheme {
        AfternoteFieldContainer(
            onClick = {},
            enabled = false,
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            Text(
                text = "비활성 필드",
                style = AfternoteDesign.typography.bodyLargeR,
            )
        }
    }
}
