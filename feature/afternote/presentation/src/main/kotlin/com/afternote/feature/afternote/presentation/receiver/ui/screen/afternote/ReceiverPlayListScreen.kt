package com.afternote.feature.afternote.presentation.receiver.ui.screen.afternote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreen
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

@Composable
fun MemorialPlaylistScreen(
    modifier: Modifier = Modifier,
    songs: List<PlaylistSongDisplay>,
    onBackClick: () -> Unit,
) {
    SongPlaylistScreen(
        modifier = modifier,
        title = "추모 플레이리스트",
        onBackClick = onBackClick,
        songs = songs,
        defaultBottomNavTab = BottomNavTab.NOTE,
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewMemorialPlaylist() {
    MaterialTheme {
        MemorialPlaylistScreen(
            songs =
                (0..9).map { i ->
                    PlaylistSongDisplay(id = "$i", title = "노래 제목", artist = "가수 이름")
                },
            onBackClick = {},
        )
    }
}
