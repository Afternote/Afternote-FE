package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistScreenScreenshot() {
    AfternoteTheme {
        MemorialPlaylistScreen(
            songs =
                listOf(
                    PlaylistSongDisplay(id = "1", title = "노래 1", artist = "아티스트 1"),
                    PlaylistSongDisplay(id = "2", title = "노래 2", artist = "아티스트 2"),
                ),
            onBackClick = {},
        )
    }
}
