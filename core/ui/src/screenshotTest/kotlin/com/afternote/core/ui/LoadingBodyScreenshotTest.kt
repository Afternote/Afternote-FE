package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/** [LoadingBody]의 전체 화면 중앙 40dp 로딩 인디케이터 baseline. */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun loadingBodyScreenshot() {
    AfternoteTheme {
        LoadingBody()
    }
}
