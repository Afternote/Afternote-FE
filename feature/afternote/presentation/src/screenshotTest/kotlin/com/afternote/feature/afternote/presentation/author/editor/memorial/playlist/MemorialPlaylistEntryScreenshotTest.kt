package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistEntryEmptyScreenshot() {
    AfternoteTheme {
        MemorialPlaylistEntry(songs = emptyList())
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistEntryWithSongsScreenshot() {
    AfternoteTheme {
        MemorialPlaylistEntry(
            songs =
                listOf(
                    Song(id = "1", title = "노래 제목 1", artist = "아티스트 1", albumCoverUrl = null),
                    Song(id = "2", title = "노래 제목 2", artist = "아티스트 2", albumCoverUrl = null),
                ),
        )
    }
}
