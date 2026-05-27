package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun homeHeaderSectionScreenshot() {
    AfternoteTheme {
        HomeHeaderSection(
            nextStepText = "다음 단계 진행하기",
            onNextStepClick = {},
        )
    }
}
