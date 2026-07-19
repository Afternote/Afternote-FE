package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun detailLoadErrorContentRawMessageScreenshot() {
    AfternoteTheme {
        DetailLoadErrorContent(
            rawMessage = "서버 점검 중입니다. 잠시 후 다시 시도해 주세요.",
            messageRes = null,
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun detailLoadErrorContentMessageResFallbackScreenshot() {
    AfternoteTheme {
        DetailLoadErrorContent(
            rawMessage = null,
            messageRes = R.string.afternote_detail_load_error,
            onBackClick = {},
        )
    }
}
