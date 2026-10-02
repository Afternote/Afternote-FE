package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [SectionGoButton] 의 시각 회귀 baseline — 둥근 알약 회색 버튼.
 *
 * 의도된 시각 변경 시 `./gradlew :app:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun sectionGoButtonScreenshot() {
    AfternoteTheme {
        SectionGoButton(
            text = "마음의 기록 확인하러 가기",
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
