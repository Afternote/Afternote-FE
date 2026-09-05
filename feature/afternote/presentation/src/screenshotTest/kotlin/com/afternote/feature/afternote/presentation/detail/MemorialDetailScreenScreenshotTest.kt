package com.afternote.feature.afternote.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialDetailScreenScreenshot() {
    AfternoteTheme {
        MemorialDetailScreen(
            onBackClick = {},
            onEditClick = {},
            onDeleteConfirm = {},
            onVideoClick = {},
            content =
                MemorialDetailContent(
                    finalWriteDate = "2025.11.26",
                ),
            userName = "서영",
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialDetailScreenWithVideoScreenshot() {
    AfternoteTheme {
        MemorialDetailScreen(
            onBackClick = {},
            onEditClick = {},
            onDeleteConfirm = {},
            onVideoClick = {},
            content =
                MemorialDetailContent(
                    finalWriteDate = "2025.11.26",
                    memorialVideoUrl = "https://cdn.example.com/memorial.mp4",
                ),
            userName = "서영",
        )
    }
}
