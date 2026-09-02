package com.afternote.feature.afternote.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun galleryDetailScreenScreenshot() {
    AfternoteTheme {
        GalleryDetailScreen(
            onBackClick = {},
            onEditClick = {},
            onDeleteConfirm = {},
            content =
                GalleryDetailContent(
                    serviceName = "직접 입력한 갤러리 서비스",
                    finalWriteDate = "2026.08.28",
                ),
        )
    }
}
