package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.model.AlbumCover
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialReceivedDetailScreenScreenshot() {
    AfternoteTheme {
        MemorialReceivedDetailScreen(
            senderName = "서연",
            albumCovers =
                listOf(
                    AlbumCover(id = "1"),
                    AlbumCover(id = "2"),
                    AlbumCover(id = "3"),
                ),
            songCount = 16,
        )
    }
}
