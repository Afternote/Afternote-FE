package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.detail.PlaylistEmptyContent
import com.afternote.feature.afternote.presentation.shared.detail.PlaylistSongCountHeader
import com.afternote.feature.afternote.presentation.shared.detail.PlaylistSongList
import com.afternote.feature.afternote.presentation.shared.detail.SongPlaylistScaffold
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * 수신자 추억 노트 플레이리스트 전체보기 화면 (view-only).
 *
 * 시안: 전체보기 타이틀은 "○○님의 플레이리스트" 로 발신자 이름을 개인화한다
 * (발신자 화면의 "추억 플레이리스트" 와 달리 유족 관점, #274).
 *
 * 공용 부품([SongPlaylistScaffold] + [PlaylistSongList])을 직접 조립한다. 헤더는 발신자 열람 화면
 * (MemorialPlaylistEntry 목록 모드)과 같은 [PlaylistSongCountHeader] 다 — 곡 검색 헤더는 곡을 골라 담는 화면
 * (AddSongScreen)의 것이라, 열람만 하는 수신자에게는 고인의 플레이리스트에 곡을 더할 수 있는 것처럼
 * 보였다 (#620).
 */
@Composable
fun MemorialPlaylistScreen(
    songs: List<PlaylistSongDisplay>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    senderName: String = "",
) {
    SongPlaylistScaffold(
        title = stringResource(R.string.afternote_receiver_memorial_playlist_screen_title, senderName),
        onBackClick = onBackClick,
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        ) {
            PlaylistSongList(
                modifier = Modifier.fillMaxSize(),
                songs = songs,
                header = { PlaylistSongCountHeader(songCount = songs.size) },
            )
            if (songs.isEmpty()) {
                PlaylistEmptyContent(
                    text = stringResource(R.string.afternote_receiver_playlist_empty_message),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
