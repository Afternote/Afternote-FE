package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun homeHeaderSectionScreenshot() {
    AfternoteTheme {
        HomeHeaderSection(
            description = stringResource(R.string.afternote_home_header_description),
            nextStepText = "다음 단계 진행하기",
            onNextStepClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun homeHeaderSectionNoNextStepScreenshot() {
    AfternoteTheme {
        HomeHeaderSection(
            description = stringResource(R.string.afternote_home_header_description),
            nextStepText = "",
            onNextStepClick = {},
        )
    }
}
