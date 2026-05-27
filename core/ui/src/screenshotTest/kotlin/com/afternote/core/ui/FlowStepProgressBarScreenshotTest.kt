package com.afternote.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.FlowStepProgressBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [FlowStepProgressBar] 의 중간 단계 진행률 baseline (`currentStep = 2, totalSteps = 4` → 50%).
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun flowStepProgressBarMidScreenshot() {
    AfternoteTheme {
        FlowStepProgressBar(
            currentStep = 2,
            totalSteps = 4,
            modifier = Modifier.padding(16.dp),
        )
    }
}
