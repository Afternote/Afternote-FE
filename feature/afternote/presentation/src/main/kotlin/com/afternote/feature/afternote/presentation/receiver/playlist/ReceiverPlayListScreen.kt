package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScreen
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 수신자 추억 노트 플레이리스트 전체보기 화면.
 *
 * 시안: 전체보기 타이틀은 "○○님의 플레이리스트" 로 발신자 이름을 개인화한다
 * (발신자 화면의 "추억 플레이리스트" 와 달리 유족 관점, #274).
 */
@Composable
fun MemorialPlaylistScreen(
    songs: List<PlaylistSongDisplay>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    senderName: String = "",
) {
    SongPlaylistScreen(
        modifier = modifier,
        title = stringResource(R.string.receiver_memorial_playlist_screen_title, senderName),
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
            senderName = "서연",
        )
    }
}
