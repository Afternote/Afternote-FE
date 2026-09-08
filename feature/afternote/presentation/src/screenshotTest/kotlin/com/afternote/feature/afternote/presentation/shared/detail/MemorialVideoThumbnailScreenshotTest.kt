package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/** 다크 테마에서도 시안의 고정 검정 스크림과 흰 전경색을 유지하는지 검증한다 (#463). */
@PreviewTest
@Preview(
    showBackground = true,
    backgroundColor = 0xFFBDBDBD,
    widthDp = 350,
    heightDp = 183,
)
@Composable
internal fun memorialVideoThumbnailDarkScreenshot() {
    AfternoteTheme(isDarkTheme = true) {
        MemorialVideoThumbnail(thumbnailUrl = null)
    }
}
