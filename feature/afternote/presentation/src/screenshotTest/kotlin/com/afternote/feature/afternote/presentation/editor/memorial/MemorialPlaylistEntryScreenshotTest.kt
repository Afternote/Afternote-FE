package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

private fun screenshotSongs(): List<Song> =
    listOf(
        Song(
            selectionKey = "screenshot:1",
            title = "노래 제목 1",
            artist = "아티스트 1",
            albumCoverUrl = null,
        ),
        Song(
            selectionKey = "screenshot:2",
            title = "노래 제목 2",
            artist = "아티스트 2",
            albumCoverUrl = null,
        ),
    )

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistEntryEmptyScreenshot() {
    AfternoteTheme {
        MemorialPlaylistEntry(
            songs = emptyList(),
            onBackClick = {},
            onNavigateToAddSongScreen = {},
            onClearAllSongs = {},
            onRemoveSongs = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistEntryWithSongsScreenshot() {
    AfternoteTheme {
        MemorialPlaylistEntry(
            songs = screenshotSongs(),
            onBackClick = {},
            onNavigateToAddSongScreen = {},
            onClearAllSongs = {},
            onRemoveSongs = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistEntryEditModeScreenshot() {
    AfternoteTheme {
        MemorialPlaylistEntry(
            songs = screenshotSongs(),
            onBackClick = {},
            onNavigateToAddSongScreen = {},
            onClearAllSongs = {},
            onRemoveSongs = {},
            initialEditMode = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistEntryEditModeSelectionScreenshot() {
    AfternoteTheme {
        MemorialPlaylistEntry(
            songs = screenshotSongs(),
            onBackClick = {},
            onNavigateToAddSongScreen = {},
            onClearAllSongs = {},
            onRemoveSongs = {},
            initialEditMode = true,
            initialSelectedSongKeys = setOf("screenshot:1"),
        )
    }
}
