package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.song.PlaylistSongList
import com.afternote.feature.afternote.presentation.shared.detail.song.SongPlaylistScaffold
import com.afternote.feature.afternote.presentation.shared.detail.song.SongSearchSection
import com.afternote.feature.afternote.presentation.shared.detail.song.filterSongsByQuery
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 수신자 추억 노트 플레이리스트 전체보기 화면 (view-only).
 *
 * 시안: 전체보기 타이틀은 "○○님의 플레이리스트" 로 발신자 이름을 개인화한다
 * (발신자 화면의 "추억 플레이리스트" 와 달리 유족 관점, #274).
 *
 * 공용 부품([SongPlaylistScaffold] + [PlaylistSongList])을 직접 조립한다. 검색은 로컬 —
 * 이미 받은 목록을 클라 문자열로 좁혀 넘긴다 (필터 소유권은 리스트가 아닌 이 화면).
 */
@Composable
fun MemorialPlaylistScreen(
    songs: List<PlaylistSongDisplay>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    senderName: String = "",
) {
    var searchQuery by remember { mutableStateOf("") }
    // remember(songs, searchQuery): 두 키가 이전 recomposition 과 == 로 같으면 캐시 반환, 하나라도
    // 바뀌면 재필터 — 무관한 recomposition 의 낭비 재계산 방지 (키=계산의 입력 전부).
    val visibleSongs = remember(songs, searchQuery) { filterSongsByQuery(songs, searchQuery) }

    SongPlaylistScaffold(
        title = stringResource(R.string.receiver_memorial_playlist_screen_title, senderName),
        onBackClick = onBackClick,
        modifier = modifier,
    ) { paddingValues ->
        PlaylistSongList(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            songs = visibleSongs,
            header = {
                SongSearchSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                )
            },
        )
    }
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
